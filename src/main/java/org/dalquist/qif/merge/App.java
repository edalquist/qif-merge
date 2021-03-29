package org.dalquist.qif.merge;

import static java.lang.System.out;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.dalquist.qif.model.Account;
import org.dalquist.qif.model.Account.AccountBlock;
import org.dalquist.qif.model.Document;
import org.dalquist.qif.model.Header;
import org.dalquist.qif.model.OptionAutoSwitch;
import org.dalquist.qif.model.TypeBank;
import org.dalquist.qif.model.TypeBank.BankBlock.SplitBlock;
import org.dalquist.qif.model.TypeCategoryList;
import org.dalquist.qif.model.TypeCategoryList.CategoryBlock;
import org.dalquist.qif.model.TypeClass;

/**
 * Reads a bunch of YNAB exported QIF files and merges them into a single QIF
 * file for import into Moneydancer.
 */
public class App {
    private static final Pattern FILE_ACCOUNT_NAME = Pattern.compile("[^-]+-(.*) as of.*");

    private static ImmutableMap<String, String> getAccountTypeMap() throws Exception {
        Path accountMapFile = Path.of("/Users/edalquist/Downloads/YNAB_Exports/account_map.csv");
        try (Stream<String> lines = Files.lines(accountMapFile, Charset.defaultCharset())) {
            return lines.map(Splitter.on(",")::splitToList)
                    .collect(ImmutableMap.toImmutableMap(l -> l.get(0), l -> l.get(1)));
        }
    }

    public static void main(String[] args) throws Exception {
        Path bankDir = Path.of("/Users/edalquist/Downloads/YNAB_Exports/qif_export");

        ImmutableMap<String, String> accountTypeMap = getAccountTypeMap();

        // Read all account files into named groups of qif data
        LinkedHashMap<String, List<Header<?>>> accounts = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(bankDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".qif")).forEach(p -> {
                Matcher matcher = FILE_ACCOUNT_NAME.matcher(p.getFileName().toString());
                matcher.find();
                String accountName = matcher.toMatchResult().group(1);
                out.println("Parsing " + accountName + " from: " + p);
                accounts.put(accountName, Document.parse(p).sections);
            });
        }

        // Ensure each account file starts with an Account header
        accounts.entrySet().forEach(e -> {
            List<Header<?>> headers = e.getValue();
            Account account;
            if (headers.isEmpty() || !(e.getValue().get(0) instanceof Account)) {
                account = new Account();
                account.getOrCreateBlock().setName(e.getKey());
                headers = ImmutableList.<Header<?>>builder().add(account).addAll(headers).build();
                e.setValue(headers);
            } else {
                account = (Account) e.getValue().get(0);
            }
            AccountBlock accountBlock = account.getOrCreateBlock();
            if (!e.getKey().equalsIgnoreCase(accountBlock.getName())) {
                throw new IllegalArgumentException("File name '" + e.getKey()
                        + "' doesn't match embedded account name '" + accountBlock.getName() + "'");
            }
            String type = accountTypeMap.getOrDefault(accountBlock.getName(), accountBlock.getType());
            if (Strings.isNullOrEmpty(type)) {
                type = "Bank";
            }
            accountBlock.setType(type);
            out.println(accountBlock.getName() + "," + accountBlock.getType());
        });

        // Fix YNAB4 transfers to work in Moneydance by extracting account names and
        // populating in the category field
        accounts.values().stream().flatMap(List::stream).filter(TypeBank.class::isInstance).map(TypeBank.class::cast)
                .map(TypeBank::getBlocks).flatMap(List::stream).forEach(bb -> {
                    String payee = bb.getPayee();
                    if (payee != null && payee.startsWith("Transfer : ")) {
                        String destAccount = payee.substring("Transfer : ".length());

                        if (accounts.containsKey(destAccount)) {
                            String category = bb.getCategory();
                            bb.setCategory("[" + destAccount + "]");
                            if (category != null) {
                                if (bb.getMemo() == null) {
                                    bb.setMemo(category);
                                } else {
                                    bb.setMemo(bb.getMemo() + " - " + category);
                                }
                            }
                        }
                    }
                });

        Path destFile = bankDir.getParent().resolve("output").resolve("qif_merged.qif");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(destFile, Charset.defaultCharset()))) {
            // Budget name + desc
            writer.println(new TypeClass("Personal Finance", "YNAB4 Import").toString());

            // Collect categories
            TypeCategoryList categoryList = getCategoryList(accounts);
            writer.println(categoryList.toString());

            // Switch to Accounts
            writer.print(new OptionAutoSwitch());

            // Account List
            accounts.values().stream().map(l -> l.get(0)).filter(Account.class::isInstance).map(Account.class::cast)
                    .sorted(Comparator.comparing(a -> a.getOrCreateBlock().getName())).map(Account::toString)
                    .forEach(writer::println);

            // Account + Bank pairs
            accounts.values().stream().flatMap(List::stream).map(Header::toString).forEach(writer::println);
        }
    }

    private static TypeCategoryList getCategoryList(Map<String, List<Header<?>>> accounts) {
        Stream<CategoryBlock> catgories = accounts.values().stream().flatMap(List::stream)
                .filter(h -> h instanceof TypeBank).map(TypeBank.class::cast).map(TypeBank::getBlocks)
                // Convert BankBlocks to category strings
                .flatMap(List::stream).flatMap(b -> {
                    if (b.getSplitBlocks().isEmpty()) {
                        return Stream.of(b.getCategory());
                    }
                    return b.getSplitBlocks().stream().map(SplitBlock::getCategoryInSplit);
                })
                // Filter null/empty strings then get the unique sorted set
                .filter(Objects::nonNull).filter(Predicate.not(String::isBlank))
                .filter(c -> !c.startsWith("[") && !c.endsWith("]")).distinct().sorted()
                // Create CategoryBlocks
                .map(categoryName -> {
                    TypeCategoryList.CategoryBlock categoryBlock = new TypeCategoryList.CategoryBlock();
                    categoryBlock.setName(categoryName);
                    categoryBlock.setDescription(categoryName);
                    if (categoryName.startsWith("Income:")) {
                        categoryBlock.setIncome(true);
                    } else {
                        categoryBlock.setExpense(true);
                    }
                    return categoryBlock;
                });
        return new TypeCategoryList(catgories);
    }
}

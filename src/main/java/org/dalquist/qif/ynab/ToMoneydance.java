package org.dalquist.qif.ynab;

import static java.lang.System.out;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Streams;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.dalquist.qif.model.Account;
import org.dalquist.qif.model.Header;
import org.dalquist.qif.model.OptionAutoSwitch;
import org.dalquist.qif.model.TypeBank;
import org.dalquist.qif.model.TypeCategoryList;
import org.dalquist.qif.model.TypeClass;
import org.dalquist.qif.model.Account.AccountBlock;
import org.dalquist.qif.model.TypeBank.BankBlock;
import org.dalquist.qif.model.TypeBank.BankBlock.SplitBlock;
import org.dalquist.qif.model.TypeCategoryList.CategoryBlock;

public class ToMoneydance {
  private static final String TRANSFER_PREFIX = "Transfer : ";
  private static final String SPLIT_TRANSFER_AFFIX = " / Transfer : ";

  private static final Pattern SPLIT_PATTERN = Pattern.compile("\\(Split ([0-9]+)/([0-9]+)\\)(?: ([^\"]*))?");

  private static Supplier<ImmutableMap<String, String>> ACCOUNT_TYPE_MAP = Suppliers.memoize(() -> {
    Path accountMapFile = Path.of("/Users/edalquist/Downloads/YNAB_Exports/account_map.csv");

    try (CSVParser parser = CSVParser.parse(accountMapFile.toFile(), Charset.defaultCharset(), CSVFormat.DEFAULT)) {
      return StreamSupport.stream(parser.spliterator(), false)
          .collect(ImmutableMap.toImmutableMap(l -> l.get(0), l -> l.get(1)));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  });

  public static void main(String[] args) throws Exception {
    Path ynabExport = Path.of("/Users/edalquist/Downloads/YNAB_Exports/E&G as of 2021-02-21 1100 AM-Register.csv");

    LinkedHashMap<String, CategoryBlock> categories = new LinkedHashMap<>();
    LinkedHashMap<String, Account> accounts = new LinkedHashMap<>();
    LinkedHashMap<String, TypeBank> accountRegisters = new LinkedHashMap<>();

    try (CSVParser parser = CSVParser.parse(ynabExport.toFile(), Charset.defaultCharset(),
        CSVFormat.DEFAULT.withHeader())) {
      Map<String, Integer> headerMap = parser.getHeaderMap();
      out.println(headerMap);

      for (Iterator<CSVRecord> csvItr = parser.iterator(); csvItr.hasNext();) {
        CSVRecord l = csvItr.next();
        String accountName = l.get(0); // "Account");
        Account account = accounts.computeIfAbsent(accountName, ToMoneydance::createAccount);
        accountName = account.getBlock().getName();

        TypeBank bank = accountRegisters.computeIfAbsent(accountName, ToMoneydance::createBank);

        BankBlock bankBlock = bank.addBankBlock();
        bankBlock.setNumber(l.get("Check Number"));
        bankBlock.setDate(l.get("Date"));
        String payee = l.get("Payee");
        String category = l.get("Category");
        String memo = l.get("Memo");

        if (payee.startsWith(TRANSFER_PREFIX)) {
          memo = mergeSplitCategory(category, memo);
          category = "[" + payee.substring(TRANSFER_PREFIX.length()) + "]";
        } else {
          bankBlock.setPayee(payee);
          categories.computeIfAbsent(category, ToMoneydance::createCategoryBlock);
        }
        bankBlock.setClearedStatus(l.get("Cleared"));

        String ammount = parseMoney(l.get("Inflow")).subtract(parseMoney(l.get("Outflow"))).toString();

        Matcher splitMatcher = SPLIT_PATTERN.matcher(memo);
        if (splitMatcher.find()) {
          int splitStart = Integer.valueOf(splitMatcher.group(1));
          Preconditions.checkState(1 == splitStart, "Splits should start at 1:\n" + l);

          int splitTotal = Integer.valueOf(splitMatcher.group(2));
          String splitMemo = splitMatcher.group(3);

          SplitBlock splitBlock = bankBlock.addSplitBlock();
          splitBlock.setCategoryInSplit(category);
          splitBlock.setDollarAmmount(ammount);
          splitBlock.setMemoInSplit(splitMemo);

          categories.computeIfAbsent(category, ToMoneydance::createCategoryBlock);

          int splitXfrIdx = payee.indexOf(SPLIT_TRANSFER_AFFIX);
          if (splitXfrIdx > 0) {
            String splitPayee = payee.substring(0, splitXfrIdx);
            if ("null".equals(splitPayee)) {
              splitPayee = "";
            }
            bankBlock.setPayee(splitPayee);

            // TODO null/empty checks
            splitBlock.setMemoInSplit(splitBlock.getCategoryInSplit() + " / " + splitBlock.getMemoInSplit());

            String destAccount = payee.substring(splitXfrIdx + SPLIT_TRANSFER_AFFIX.length());
            splitBlock.setCategoryInSplit("[" + destAccount + "]");
          }

          // Iterate through split blocks
          for (splitStart++; splitStart <= splitTotal; splitStart++) {
            l = csvItr.next();
            Preconditions.checkState(accountName.equals(l.get(0)/* "Account") */), "Expected account %s:\n%s",
                accountName, l);
            Preconditions.checkState(bankBlock.getDate().equals(l.get("Date")), "Expected date %s:\n%s",
                bankBlock.getDate(), l);

            memo = l.get("Memo");
            splitMatcher = SPLIT_PATTERN.matcher(memo);
            Preconditions.checkState(splitMatcher.find(), "Expected split memo:\n%s", l);
            Preconditions.checkState(splitStart == Integer.parseInt(splitMatcher.group(1)), "Expected split %s:\n%s",
                splitStart, l);
            Preconditions.checkState(splitTotal == Integer.parseInt(splitMatcher.group(2)),
                "Expected split total %s:\n%s", splitTotal, l);

            category = l.get("Category");
            ammount = parseMoney(l.get("Inflow")).subtract(parseMoney(l.get("Outflow"))).toString();
            splitMemo = splitMatcher.group(3);

            // Add new split
            splitBlock = bankBlock.addSplitBlock();

            splitBlock.setCategoryInSplit(category);
            splitBlock.setDollarAmmount(ammount);
            splitBlock.setMemoInSplit(splitMemo);

            payee = l.get("Payee");
            splitXfrIdx = payee.indexOf(SPLIT_TRANSFER_AFFIX);
            if (splitXfrIdx > 0) {
              String splitPayee = payee.substring(0, splitXfrIdx);
              if ("null".equals(splitPayee)) {
                splitPayee = "";
              }
              bankBlock.setPayee(splitPayee);

              // Build split memo using
              String memoInSplit = mergeSplitCategory(splitBlock.getCategoryInSplit(), splitBlock.getMemoInSplit());
              splitBlock.setMemoInSplit(memoInSplit.toString());

              String destAccount = payee.substring(splitXfrIdx + SPLIT_TRANSFER_AFFIX.length());
              splitBlock.setCategoryInSplit("[" + destAccount + "]");
            } else {
              bankBlock.setPayee(payee);
            }
          }
        } else {
          bankBlock.setCategory(category);
          bankBlock.setMemo(memo);
          bankBlock.setAmmount(ammount);
        }
      }
    }

    Path destFile = ynabExport.resolveSibling("output").resolve("csv_merged.qif");
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(destFile, Charset.defaultCharset()))) {
      // Budget name + desc
      writer.println(new TypeClass("Personal Finance", "YNAB4 Import").toString());

      // Collect categories
      TypeCategoryList categoryList = new TypeCategoryList();
      categoryList.addBlocks(categories.values());
      writer.println(categoryList.toString());

      // Switch to Accounts
      writer.print(new OptionAutoSwitch());

      // Account List
      accounts.values().stream().sorted(Comparator.comparing(a -> a.getOrCreateBlock().getName()))
          .map(Account::toString).forEach(writer::println);

      // Account + Bank pairs
      accountRegisters.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
        Account account = accounts.get(e.getKey());
        writer.println(account);
        writer.println(e.getValue());
      });
    }
  }

  private static String mergeSplitCategory(String category, String memo) {
    StringBuilder memoInSplit = new StringBuilder();
    if (!Strings.isNullOrEmpty(category)) {
      memoInSplit.append(category);
    }
    if (!Strings.isNullOrEmpty(memo)) {
      if (memoInSplit.length() > 0) {
        memoInSplit.append(" / ");
      }
      memoInSplit.append(memo);
    }
    return memoInSplit.toString();
  }

  private static BigDecimal parseMoney(String money) {
    Preconditions.checkArgument(money.startsWith("$"), "Money expression should start with $: " + money);
    return new BigDecimal(money.substring(1));
  }

  private static Account createAccount(String accountName) {
    if (Strings.isNullOrEmpty(accountName)) {
      accountName = "UNKNOWN";
    }

    Account account = new Account();
    AccountBlock accountBlock = account.getOrCreateBlock();
    accountBlock.setName(accountName);

    String type = ACCOUNT_TYPE_MAP.get().getOrDefault(accountBlock.getName(), accountBlock.getType());
    if (Strings.isNullOrEmpty(type)) {
      type = "Bank";
    }
    accountBlock.setType(type);

    return account;
  }

  private static CategoryBlock createCategoryBlock(String categoryName) {
    if (Strings.isNullOrEmpty(categoryName)) {
      return null;
    }

    CategoryBlock categoryBlock = new CategoryBlock();
    categoryBlock.setName(categoryName);
    categoryBlock.setDescription(categoryName);
    if (categoryName.startsWith("Income:")) {
      categoryBlock.setIncome(true);
    } else {
      categoryBlock.setExpense(true);
    }
    return categoryBlock;
  }

  private static TypeBank createBank(String accountName) {
    return new TypeBank();
  }
}

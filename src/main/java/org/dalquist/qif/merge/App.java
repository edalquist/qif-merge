package org.dalquist.qif.merge;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;

import org.dalquist.qif.model.Account;
import org.dalquist.qif.model.Block;
import org.dalquist.qif.model.Document;
import org.dalquist.qif.model.Header;
import org.dalquist.qif.model.HeaderLine;
import org.dalquist.qif.model.OptionAutoSwitch;
import org.dalquist.qif.model.TypeBank;
import org.dalquist.qif.model.TypeCategoryList;
import org.dalquist.qif.model.TypeClass;

/**
 * Hello world!
 */
public class App {

    private static final Pattern FILE_ACCOUNT_NAME = Pattern.compile("[^-]+-(.*) as of.*");

    public static void main(String[] args) throws Exception {
        Path bankDir = Path.of("/Users/edalquist/Downloads/Exports/banking");

        Map<String, List<Header>> accounts = new HashMap<>();
        Files.list(bankDir).filter(p -> p.getFileName().toString().endsWith(".qif")).forEach(p -> {
            Matcher matcher = FILE_ACCOUNT_NAME.matcher(p.getFileName().toString());
            matcher.find();
            String accountName = matcher.toMatchResult().group(0);
            System.out.println("Parsing " + accountName + " from: " + p);
            accounts.put(accountName, Document.parse(p).sections);
        });

        System.out.println(accounts.values().stream().flatMap(List::stream).map(Header::toString)
                .collect(Collectors.joining("\n")));

        // TODO build up Moneydance single-qif output file
    }
}

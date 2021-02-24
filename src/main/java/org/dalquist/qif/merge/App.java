package org.dalquist.qif.merge;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;

import org.dalquist.qif.model.Account;
import org.dalquist.qif.model.Block;
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
    private static final ImmutableMap<String, Class<? extends Header>> QIF_HEADERS = Stream
            .of(TypeClass.class, TypeCategoryList.class, OptionAutoSwitch.class, Account.class, TypeBank.class)
            .collect(ImmutableMap.toImmutableMap(h -> h.getAnnotationsByType(HeaderLine.class)[0].value(),
                    Function.identity()));

    public static void main(String[] args) throws Exception {
        Path qifFile =
                // Path.of("/Users/edalquist/Downloads/Exports/Personal Finances.qif");
                Path.of("/Users/edalquist/Downloads/Exports/E&G-Chase Amazon Visa as of 2021-02-21 950 AM.qif");
        List<String> lines = Files.readAllLines(qifFile, Charset.defaultCharset());
        List<Header> parsedFile = new ArrayList<>();
        Header current = null;

        LinkedListMultimap<Character, String> blockLines = LinkedListMultimap.create();

        for (String line : lines) {
            if (line.startsWith("!")) {
                Class<? extends Header> headerType = QIF_HEADERS.get(line);
                if (headerType == null) {
                    System.out.println(parsedFile.stream().map(Header::toString).collect(Collectors.joining("\n")));
                    throw new IllegalStateException("No Header found for: " + line);
                }

                current = headerType.getConstructor().newInstance();
                parsedFile.add(current);
                blockLines.clear();
            } else if (current != null && Block.BLOCK_END_LINE.equals(line)) {
                current.parseBlock(blockLines);
                blockLines.clear();
            } else {
                blockLines.put(line.charAt(0), line.substring(1));

            }
        }

        System.out.println(parsedFile.stream().map(Header::toString).collect(Collectors.joining("\n")));
    }
}

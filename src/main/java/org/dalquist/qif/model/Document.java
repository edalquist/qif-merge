package org.dalquist.qif.model;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;

public final class Document {
  private static final ImmutableMap<String, Class<? extends Header>> QIF_HEADERS = Stream
      .of(TypeClass.class, TypeCategoryList.class, OptionAutoSwitch.class, Account.class, TypeBank.class).collect(
          ImmutableMap.toImmutableMap(h -> h.getAnnotationsByType(HeaderLine.class)[0].value(), Function.identity()));

  public static Document parse(Path qifFile) {
    try {
      ImmutableList.Builder<Header> sectionsBuilder = ImmutableList.builder();
      Header current = null;
      LinkedListMultimap<Character, String> blockLines = LinkedListMultimap.create();

      for (Iterator<String> lineItr = Files.lines(qifFile, Charset.defaultCharset()).iterator(); lineItr.hasNext();) {
        String line = lineItr.next();
        if (line.startsWith("!")) {
          Class<? extends Header> headerType = QIF_HEADERS.get(line);
          if (headerType == null) {
            throw new IllegalStateException("No Header found for: " + line);
          }

          current = headerType.getConstructor().newInstance();
          sectionsBuilder.add(current);
          blockLines.clear();
        } else if (current != null && Block.BLOCK_END_LINE.equals(line)) {
          current.parseBlock(blockLines);
          blockLines.clear();
        } else {
          blockLines.put(line.charAt(0), line.substring(1));
        }
      }
      return new Document(sectionsBuilder.build());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public ImmutableList<Header> sections;

  private Document(List<Header> sections) {
    this.sections = ImmutableList.copyOf(sections);
  }
}

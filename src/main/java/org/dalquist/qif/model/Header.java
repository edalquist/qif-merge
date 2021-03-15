package org.dalquist.qif.model;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.LinkedListMultimap;

public abstract class Header<T extends Block> {
  public static final String HEADER_PREFIX = "!";

  public abstract void parseBlock(LinkedListMultimap<Character, String> lines);

  public abstract List<T> getBlocks();

  @Override
  public final String toString() {
    return this.getClass().getAnnotationsByType(HeaderLine.class)[0].value() + '\n'
        + getBlocks().stream().map(Block::toString).collect(Collectors.joining("\n"));
  }
}

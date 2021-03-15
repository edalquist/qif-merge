package org.dalquist.qif.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;

public abstract class Block {
  public static final String BLOCK_END_LINE = "^";

  static final Character getFieldPrefix(Field f) {
    String value = f.getAnnotation(FieldPrefix.class).value();
    Preconditions.checkArgument(value.length() == 1, "%s has a prefix that is not exactly 1 character: %s", f, value);
    return value.charAt(0);
  }

  static final Stream<Field> getPrefixedFieldStream(Class<? extends Block> c) {
    return Stream.of(c.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers()))
        .filter(f -> f.getAnnotation(FieldPrefix.class) != null);
  }

  private final ImmutableMap<Character, Field> fieldsByPrefix = getPrefixedFieldStream(this.getClass())
      .collect(ImmutableMap.toImmutableMap(Block::getFieldPrefix, f -> {
        f.setAccessible(true);
        return f;
      }));

  private final ImmutableMap<Character, Class<? extends Block>> subBlocksByPrefix = fieldsByPrefix.values().stream()
      .filter(f -> !Block.class.equals(f.getAnnotation(FieldPrefix.class).blockType())).collect(
          ImmutableMap.toImmutableMap(f -> getFieldPrefix(f), f -> f.getAnnotation(FieldPrefix.class).blockType()));

  private final ImmutableMap<Character, ImmutableList<Character>> subBlockFieldsByPrefix = subBlocksByPrefix.entrySet()
      .stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, v -> getPrefixedFieldStream(v.getValue())
          .map(Block::getFieldPrefix).filter(p -> !p.equals(v.getKey())).collect(ImmutableList.toImmutableList())));

  private final ImmutableSet<Character> printByDefaultFields = fieldsByPrefix.entrySet().stream()
      .filter(e -> e.getValue().getAnnotation(FieldPrefix.class).printWhenEmpty()).map(Map.Entry::getKey)
      .collect(ImmutableSet.toImmutableSet());

  Block() {
  }

  Block(LinkedListMultimap<Character, String> lines) {
    for (ListIterator<Map.Entry<Character, String>> linesItr = lines.entries().listIterator(); linesItr.hasNext();) {
      Map.Entry<Character, String> line = linesItr.next();
      Character k = line.getKey();
      String v = line.getValue();

      Field field = fieldsByPrefix.get(k);
      if (field == null) {
        throw new IllegalStateException("No field for prefix " + k + " on " + getClass().getName());
      }

      try {
        if (subBlocksByPrefix.containsKey(k)) {
          Block subBlock = createSubBlock(linesItr, k, v);

          if (List.class.isAssignableFrom(field.getType())) {
            List<Block> subBlocks = (List<Block>) field.get(this);
            if (subBlocks == null) {
              subBlocks = new ArrayList<>();
              field.set(this, subBlocks);
            }
            subBlocks.add(subBlock);
          } else {
            field.set(this, subBlock);
          }
        } else if (Boolean.class.equals(field.getType())) {
          Preconditions.checkArgument(v.isEmpty(), "Expected empty value for field: " + k + "\n" + lines);
          field.set(this, true);
        } else {
          field.set(this, v);
        }
      } catch (IllegalArgumentException | IllegalAccessException | InstantiationException | InvocationTargetException
          | NoSuchMethodException | SecurityException e) {
        throw new AssertionError(e);
      }
    }
  }

  protected String getBlockEndline() {
    return BLOCK_END_LINE;
  }

  private Block createSubBlock(ListIterator<Map.Entry<Character, String>> linesItr, Character k, String v)
      throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
    Class<? extends Block> blockType = subBlocksByPrefix.get(k);
    ImmutableList<Character> subBlockFields = subBlockFieldsByPrefix.getOrDefault(k, ImmutableList.of());

    LinkedListMultimap<Character, String> subBlockLines = LinkedListMultimap.create();
    subBlockLines.put(k, v); // Always add the first line for a sub block

    while (linesItr.hasNext()) {
      Map.Entry<Character, String> subLine = linesItr.next();
      if (subBlockFields.contains(subLine.getKey())) {
        subBlockLines.put(subLine.getKey(), subLine.getValue());
      } else {
        linesItr.previous();
        break;
      }
    }

    Constructor<? extends Block> cstr = blockType.getDeclaredConstructor(LinkedListMultimap.class);
    Block subBlock = cstr.newInstance(subBlockLines);
    return subBlock;
  }

  @Override
  public final String toString() {
    String blockStr = fieldsByPrefix.entrySet().stream()
        .map(e -> new SimpleEntry<>(e.getKey(), safeGet(e.getValue()))).<Map.Entry<Character, Object>>flatMap(e -> {
          Object v = e.getValue();
          if (v instanceof Collection) {
            return ((Collection<?>) v).stream().map(cv -> new SimpleEntry<>(e.getKey(), cv));
          }
          return Stream.of(e);
        }).filter(e -> e.getValue() != null || printByDefaultFields.contains(e.getKey())).map(e -> {
          String valStr = Objects.toString(e.getValue(), "");
          if (subBlocksByPrefix.containsKey(e.getKey())) {
            return valStr;
          }
          return e.getKey() + valStr;
        }).collect(Collectors.joining("\n"));
    String blockEndline = getBlockEndline();
    if (blockStr.isEmpty()) {
      return blockEndline;
    }
    if (blockEndline.isEmpty()) {
      return blockStr;
    }
    return blockStr + '\n' + blockEndline;
  }

  private final Object safeGet(Field f) {
    try {
      Object value = f.get(this);
      if (Boolean.class.equals(f.getType())) {
        return Boolean.TRUE == value ? "" : null;
      }
      return value;
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }
}

package org.dalquist.qif.model;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;

@HeaderLine(Header.HEADER_PREFIX + "Type:Class")
public final class TypeClass extends Header<TypeClass.ClassBlock> {
    private ClassBlock block = null;

    public TypeClass() {
    }

    public TypeClass(String name, String description) {
        this.block = new ClassBlock(name, description);
    }

    @Override
    public void parseBlock(LinkedListMultimap<Character, String> lines) {
        if (block != null) {
            throw new IllegalStateException(this.getClass().getName() + " only supports a single block:\n" + lines);
        }
        this.block = new ClassBlock(lines);
    }

    @Override
    public List<ClassBlock> getBlocks() {
        if (block == null) {
            return ImmutableList.of();
        }
        return ImmutableList.of(block);
    }

    public static final class ClassBlock extends Block {
        @FieldPrefix("N")
        private String name;

        @FieldPrefix("D")
        private String description;

        ClassBlock(String name, String description) {
            this.name = name;
            this.description = description;
        }

        ClassBlock(LinkedListMultimap<Character, String> lines) {
            super(lines);
        }
    }
}

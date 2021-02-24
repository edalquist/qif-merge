package org.dalquist.qif.model;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;

@HeaderLine(Header.HEADER_PREFIX + "Option:AutoSwitch")
public final class OptionAutoSwitch extends Header {
    public OptionAutoSwitch() {
    }

    @Override
    public void parseBlock(LinkedListMultimap<Character, String> lines) {
        throw new IllegalStateException(this.getClass().getName() + " does not support blocks.");
    }

    @Override
    public List<Block> getBlocks() {
        return ImmutableList.of();
    }
}

package com.dioxideliteng.util.krs.notebot;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

/** Ported from Krs (com.instrumentalist.krs.utils.notebot.Note). */
public record Note(NoteBlockInstrument instrument, int level) {
}

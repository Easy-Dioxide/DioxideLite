package com.dioxideliteng.ui.terminal;

/** Limits terminal styling to DioxideLiteNG's own screens, including their native widgets. */
public interface TerminalPage {
   String terminalTitle();
   String terminalCode();
   String terminalDescription();
   String terminalStatus();
}

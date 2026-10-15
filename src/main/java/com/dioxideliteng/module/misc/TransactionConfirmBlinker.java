package com.dioxideliteng.module.misc;

import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.exploit.TransactionConfirmBlinker).
 * The Krs module itself carries no event handlers: it is a pure toggle whose state is read by
 * the Krs ModuleManager packet filter, which queues ServerboundPongPacket (transaction
 * confirms) while blinking. That filter is adapted into Blink2's own packet handlers, which
 * look this module up through the registered instance (the equivalent of Krs
 * ModuleManager.getModuleState) via shouldIncludeTransactions().
 */
public class TransactionConfirmBlinker extends Feature {

   private static TransactionConfirmBlinker instance;

   public TransactionConfirmBlinker() {
      super("Transaction Confirm Blinker", Category.MISC);
      instance = this;
   }

   /** Krs parity: ModuleManager.getModuleState(TransactionConfirmBlinker.class). */
   public static boolean shouldIncludeTransactions() {
      return instance != null && instance.isEnabled();
   }
}

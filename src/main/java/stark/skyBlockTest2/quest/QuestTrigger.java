package stark.skyBlockTest2.quest;

public enum QuestTrigger {
    BREAK_BLOCK,
    KILL_MOB,
    FISH,
    CRAFT_ITEM,
    /** Gracz postawił blok na wyspie. */
    PLACE_BLOCK,
    /** Gracz wyciągnął przetopiony przedmiot z pieca. */
    SMELT_ITEM,
    /** Gracz sfinalizował handel z wieśniakiem. */
    TRADE,
    /** Gracz napisał wiadomość na czacie. */
    SEND_CHAT,
    /** Ktoś zaakceptował zaproszenie na wyspę gracza (gracz jest właścicielem). */
    INVITE_MEMBER,
    /** Gracz odwiedził cudzą wyspę (/is visit). */
    VISIT_ISLAND,
    /** Aukcja gracza zakończyła się sprzedażą (count lub value). */
    AUCTION_SOLD,
    /** Gracz wygrał licytację BID. */
    AUCTION_WON,
    /** Gracz odblokował wyspę Netheru (jednorazowo). */
    UNLOCK_NETHER,
    /** Gracz zjadł jedzenie (w tym tort przez right-click). */
    EAT_FOOD,
    /** Gracz przeżył upadek zadający znaczące obrażenia. */
    SURVIVE_FALL,
    /** Odpala się gdy wyspa ukończy WSZYSTKIE aktywne questy dzienne (cały zestaw). */
    COMPLETE_DAILY_QUEST_SET,
    /** Odpala się gdy wyspa ukończy WSZYSTKIE aktywne questy tygodniowe (cały zestaw). */
    COMPLETE_WEEKLY_QUEST_SET,
    /** Odpala się gdy gracz zakończy animację otwierania skrzynki. */
    OPEN_CRATE,
    /** Gracz ulepszył generator (target = nazwa GeneratorType, np. COBBLESTONE). */
    UPGRADE_GENERATOR,
    /**
     * Gracz sprzedał przedmioty w sklepie GUI (nie dom aukcyjny).
     * target="" liczy transakcje, target="VALUE" liczy zarobione złoto.
     */
    SELL_TO_SHOP,
    /** Gracz powiększył wyspę (ukończył upgrade rozmiaru wyspy). */
    EXPAND_ISLAND
}
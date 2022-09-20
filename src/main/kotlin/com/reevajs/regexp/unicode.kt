package com.reevajs.regexp

import com.ibm.icu.text.UnicodeSet

private val unicodeSets = mutableMapOf<String, UnicodeSet>()

internal fun getUnicodeClass(clazz: String) = unicodeSets.getOrPut(clazz) {
    UnicodeSet("[\\p{${clazz}}]").freeze()
}

internal fun isLineSeparator(cp: Int) = cp == 0xa || cp == 0xd || cp in 0x2028..0x2029

internal fun isWhitespace(cp: Int) = cp == '\t'.code || cp in 0xb..0xc  || cp == 0xfeff || 
    isLineSeparator(cp) || cp in getUnicodeClass("Space_Separator")

@Suppress("SpellCheckingInspection")
internal val unicodePropertyAliasList = mapOf(
    "General_Category" to "General_Category",
    "gc" to "General_Category",
    "Script" to "Script",
    "sc" to "Script",
    "Script_Extensions" to "Script_Extensions",
    "scx" to "Script_Extensions",

    "ASCII" to "ASCII",
    "ASCII_Hex_Digit" to "ASCII_Hex_Digit",
    "AHex" to "ASCII_Hex_Digit",
    "Alphabetic" to "Alphabetic",
    "Alpha" to "Alphabetic",
    "Any" to "Any",
    "Assigned" to "Assigned",
    "Bidi_Control" to "Bidi_Control",
    "Bidi_C" to "Bidi_Control",
    "Bidi_Mirrored" to "Bidi_Mirrored",
    "Bidi_M" to "Bidi_Mirrored",
    "Case_Ignorable" to "Case_Ignorable",
    "CI" to "Case_Ignorable",
    "Cased" to "Cased",
    "Changes_When_Casefolded" to "Changes_When_Casefolded",
    "CWCF" to "Changes_When_Casefolded",
    "Changes_When_Casemapped" to "Changes_When_Casemapped",
    "CWCM" to "Changes_When_Casemapped",
    "Changes_When_Lowercased" to "Changes_When_Lowercased",
    "CWL" to "Changes_When_Lowercased",
    "Changes_When_NFKC_Casefolded" to "Changes_When_NFKC_Casefolded",
    "CWKCF" to "Changes_When_NFKC_Casefolded",
    "Changes_When_Titlecased" to "Changes_When_Titlecased",
    "CWT" to "Changes_When_Titlecased",
    "Changes_When_Uppercased" to "Changes_When_Uppercased",
    "CWU" to "Changes_When_Uppercased",
    "Dash" to "Dash",
    "Default_Ignorable_Code_Point" to "Default_Ignorable_Code_Point",
    "DI" to "Default_Ignorable_Code_Point",
    "Deprecated" to "Deprecated",
    "Dep" to "Deprecated",
    "Diacritic" to "Diacritic",
    "Dia" to "Diacritic",
    "Emoji" to "Emoji",
    "Emoji_Component" to "Emoji_Component",
    "EComp" to "Emoji_Component",
    "Emoji_Modifier" to "Emoji_Modifier",
    "EMod" to "Emoji_Modifier",
    "Emoji_Modifier_Base" to "Emoji_Modifier_Base",
    "EBase" to "Emoji_Modifier_Base",
    "Emoji_Presentation" to "Emoji_Presentation",
    "EPres" to "Emoji_Presentation",
    "Extended_Pictographic" to "Extended_Pictographic",
    "ExtPict" to "Extended_Pictographic",
    "Extender" to "Extender",
    "Ext" to "Extender",
    "Grapheme_Base" to "Grapheme_Base",
    "Gr_Base" to "Grapheme_Base",
    "Grapheme_Extend" to "Grapheme_Extend",
    "Gr_Ext" to "Grapheme_Extend",
    "Hex_Digit" to "Hex_Digit",
    "Hex" to "Hex_Digit",
    "IDS_Binary_Operator" to "IDS_Binary_Operator",
    "IDSB" to "IDS_Binary_Operator",
    "IDS_Trinary_Operator" to "IDS_Trinary_Operator",
    "IDST" to "IDS_Trinary_Operator",
    "ID_Continue" to "ID_Continue",
    "IDC" to "ID_Continue",
    "ID_Start" to "ID_Start",
    "IDS" to "ID_Start",
    "Ideographic" to "Ideographic",
    "Ideo" to "Ideographic",
    "Join_Control" to "Join_Control",
    "Join_C" to "Join_Control",
    "Logical_Order_Exception" to "Logical_Order_Exception",
    "LOE" to "Logical_Order_Exception",
    "Lowercase" to "Lowercase",
    "Lower" to "Lowercase",
    "Math" to "Math",
    "Noncharacter_Code_Point" to "Noncharacter_Code_Point",
    "NChar" to "Noncharacter_Code_Point",
    "Pattern_Syntax" to "Pattern_Syntax",
    "Pat_Syn" to "Pattern_Syntax",
    "Pattern_White_Space" to "Pattern_White_Space",
    "Pat_WS" to "Pattern_White_Space",
    "Quotation_Mark" to "Quotation_Mark",
    "QMark" to "Quotation_Mark",
    "Radical" to "Radical",
    "Regional_Indicator" to "Regional_Indicator",
    "RI" to "Regional_Indicator",
    "Sentence_Terminal" to "Sentence_Terminal",
    "STerm" to "Sentence_Terminal",
    "Soft_Dotted" to "Soft_Dotted",
    "SD" to "Soft_Dotted",
    "Terminal_Punctuation" to "Terminal_Punctuation",
    "Term" to "Terminal_Punctuation",
    "Unified_Ideograph" to "Unified_Ideograph",
    "UIdeo" to "Unified_Ideograph",
    "Uppercase" to "Uppercase",
    "Upper" to "Uppercase",
    "Variation_Selector" to "Variation_Selector",
    "VS" to "Variation_Selector",
    "White_Space" to "White_Space",
    "space" to "White_Space",
    "XID_Continue" to "XID_Continue",
    "XIDC" to "XID_Continue",
    "XID_Start" to "XID_Start",
    "XIDS" to "XID_Start",
)

@Suppress("SpellCheckingInspection")
internal  val unicodeValueAliasesList = mapOf(
    "Cased_Letter" to "Cased_Letter",
    "LC" to "Cased_Letter",
    "Close_Punctuation" to "Close_Punctuation",
    "Pe" to "Close_Punctuation",
    "Connector_Punctuation" to "Connector_Punctuation",
    "Pc" to "Connector_Punctuation",
    "Control" to "Control",
    "Cc" to "Control",
    "cntrl" to "Control",
    "Currency_Symbol" to "Currency_Symbol",
    "Sc" to "Currency_Symbol",
    "Dash_Punctuation" to "Dash_Punctuation",
    "Pd" to "Dash_Punctuation",
    "Decimal_Number" to "Decimal_Number",
    "Nd" to "Decimal_Number",
    "digit" to "Decimal_Number",
    "Enclosing_Mark" to "Enclosing_Mark",
    "Me" to "Enclosing_Mark",
    "Final_Punctuation" to "Final_Punctuation",
    "Pf" to "Final_Punctuation",
    "Format" to "Format",
    "Cf" to "Format",
    "Initial_Punctuation" to "Initial_Punctuation",
    "Pi" to "Initial_Punctuation",
    "Letter" to "Letter",
    "L" to "Letter",
    "Letter_Number" to "Letter_Number",
    "Nl" to "Letter_Number",
    "Line_Separator" to "Line_Separator",
    "Zl" to "Line_Separator",
    "Lowercase_Letter" to "Lowercase_Letter",
    "Ll" to "Lowercase_Letter",
    "Mark" to "Mark",
    "M" to "Mark",
    "Combining_Mark" to "Mark",
    "Math_Symbol" to "Math_Symbol",
    "Sm" to "Math_Symbol",
    "Modifier_Letter" to "Modifier_Letter",
    "Lm" to "Modifier_Letter",
    "Modifier_Symbol" to "Modifier_Symbol",
    "Sk" to "Modifier_Symbol",
    "Nonspacing_Mark" to "Nonspacing_Mark",
    "Mn" to "Nonspacing_Mark",
    "Number" to "Number",
    "N" to "Number",
    "Open_Punctuation" to "Open_Punctuation",
    "Ps" to "Open_Punctuation",
    "Other" to "Other",
    "C" to "Other",
    "Other_Letter" to "Other_Letter",
    "Lo" to "Other_Letter",
    "Other_Number" to "Other_Number",
    "No" to "Other_Number",
    "Other_Punctuation" to "Other_Punctuation",
    "Po" to "Other_Punctuation",
    "Other_Symbol" to "Other_Symbol",
    "So" to "Other_Symbol",
    "Paragraph_Separator" to "Paragraph_Separator",
    "Zp" to "Paragraph_Separator",
    "Private_Use" to "Private_Use",
    "Co" to "Private_Use",
    "Punctuation" to "Punctuation",
    "P" to "Punctuation",
    "punct" to "Punctuation",
    "Separator" to "Separator",
    "Z" to "Separator",
    "Space_Separator" to "Space_Separator",
    "Zs" to "Space_Separator",
    "Spacing_Mark" to "Spacing_Mark",
    "Mc" to "Spacing_Mark",
    "Surrogate" to "Surrogate",
    "Cs" to "Surrogate",
    "Symbol" to "Symbol",
    "S" to "Symbol",
    "Titlecase_Letter" to "Titlecase_Letter",
    "Lt" to "Titlecase_Letter",
    "Unassigned" to "Unassigned",
    "Cn" to "Unassigned",
    "Uppercase_Letter" to "Uppercase_Letter",
    "Lu" to "Uppercase_Letter",

    "Adlam" to "Adlam",
    "Adlm" to "Adlam",
    "Ahom" to "Ahom",
    "Anatolian_Hieroglyphs" to "Anatolian_Hieroglyphs",
    "Hluw" to "Anatolian_Hieroglyphs",
    "Arabic" to "Arabic",
    "Arab" to "Arabic",
    "Armenian" to "Armenian",
    "Armn" to "Armenian",
    "Avestan" to "Avestan",
    "Avst" to "Avestan",
    "Balinese" to "Balinese",
    "Bali" to "Balinese",
    "Bamum" to "Bamum",
    "Bamu" to "Bamum",
    "Bassa_Vah" to "Bassa_Vah",
    "Bass" to "Bassa_Vah",
    "Batak" to "Batak",
    "Batk" to "Batak",
    "Bengali" to "Bengali",
    "Beng" to "Bengali",
    "Bhaiksuki" to "Bhaiksuki",
    "Bhks" to "Bhaiksuki",
    "Bopomofo" to "Bopomofo",
    "Bopo" to "Bopomofo",
    "Brahmi" to "Brahmi",
    "Brah" to "Brahmi",
    "Braille" to "Braille",
    "Brai" to "Braille",
    "Buginese" to "Buginese",
    "Bugi" to "Buginese",
    "Buhid" to "Buhid",
    "Buhd" to "Buhid",
    "Canadian_Aboriginal" to "Canadian_Aboriginal",
    "Cans" to "Canadian_Aboriginal",
    "Carian" to "Carian",
    "Cari" to "Carian",
    "Caucasian_Albanian" to "Caucasian_Albanian",
    "Aghb" to "Caucasian_Albanian",
    "Chakma" to "Chakma",
    "Cakm" to "Chakma",
    "Cham" to "Cham",
    "Chorasmian" to "Chorasmian",
    "Chrs" to "Chorasmian",
    "Cherokee" to "Cherokee",
    "Cher" to "Cherokee",
    "Common" to "Common",
    "Zyyy" to "Common",
    "Coptic" to "Coptic",
    "Copt" to "Coptic",
    "Qaac" to "Coptic",
    "Cuneiform" to "Cuneiform",
    "Xsux" to "Cuneiform",
    "Cypriot" to "Cypriot",
    "Cprt" to "Cypriot",
    "Cypro_Minoan" to "Cypro_Minoan",
    "Cpmn" to "Cypro_Minoan",
    "Cyrillic" to "Cyrillic",
    "Cyrl" to "Cyrillic",
    "Deseret" to "Deseret",
    "Dsrt" to "Deseret",
    "Devanagari" to "Devanagari",
    "Deva" to "Devanagari",
    "Dives_Akuru" to "Dives_Akuru",
    "Diak" to "Dives_Akuru",
    "Dogra" to "Dogra",
    "Dogr" to "Dogra",
    "Duployan" to "Duployan",
    "Dupl" to "Duployan",
    "Egyptian_Hieroglyphs" to "Egyptian_Hieroglyphs",
    "Egyp" to "Egyptian_Hieroglyphs",
    "Elbasan" to "Elbasan",
    "Elba" to "Elbasan",
    "Elymaic" to "Elymaic",
    "Elym" to "Elymaic",
    "Ethiopic" to "Ethiopic",
    "Ethi" to "Ethiopic",
    "Georgian" to "Georgian",
    "Geor" to "Georgian",
    "Glagolitic" to "Glagolitic",
    "Glag" to "Glagolitic",
    "Gothic" to "Gothic",
    "Goth" to "Gothic",
    "Grantha" to "Grantha",
    "Gran" to "Grantha",
    "Greek" to "Greek",
    "Grek" to "Greek",
    "Gujarati" to "Gujarati",
    "Gujr" to "Gujarati",
    "Gunjala_Gondi" to "Gunjala_Gondi",
    "Gong" to "Gunjala_Gondi",
    "Gurmukhi" to "Gurmukhi",
    "Guru" to "Gurmukhi",
    "Han" to "Han",
    "Hani" to "Han",
    "Hangul" to "Hangul",
    "Hang" to "Hangul",
    "Hanifi_Rohingya" to "Hanifi_Rohingya",
    "Rohg" to "Hanifi_Rohingya",
    "Hanunoo" to "Hanunoo",
    "Hano" to "Hanunoo",
    "Hatran" to "Hatran",
    "Hatr" to "Hatran",
    "Hebrew" to "Hebrew",
    "Hebr" to "Hebrew",
    "Hiragana" to "Hiragana",
    "Hira" to "Hiragana",
    "Imperial_Aramaic" to "Imperial_Aramaic",
    "Armi" to "Imperial_Aramaic",
    "Inherited" to "Inherited",
    "Zinh" to "Inherited",
    "Qaai" to "Inherited",
    "Inscriptional_Pahlavi" to "Inscriptional_Pahlavi",
    "Phli" to "Inscriptional_Pahlavi",
    "Inscriptional_Parthian" to "Inscriptional_Parthian",
    "Prti" to "Inscriptional_Parthian",
    "Javanese" to "Javanese",
    "Java" to "Javanese",
    "Kaithi" to "Kaithi",
    "Kthi" to "Kaithi",
    "Kannada" to "Kannada",
    "Knda" to "Kannada",
    "Katakana" to "Katakana",
    "Kana" to "Katakana",
    "Kayah_Li" to "Kayah_Li",
    "Kali" to "Kayah_Li",
    "Kharoshthi" to "Kharoshthi",
    "Khar" to "Kharoshthi",
    "Khitan_Small_Script" to "Khitan_Small_Script",
    "Kits" to "Khitan_Small_Script",
    "Khmer" to "Khmer",
    "Khmr" to "Khmer",
    "Khojki" to "Khojki",
    "Khoj" to "Khojki",
    "Khudawadi" to "Khudawadi",
    "Sind" to "Khudawadi",
    "Lao" to "Lao",
    "Laoo" to "Lao",
    "Latin" to "Latin",
    "Latn" to "Latin",
    "Lepcha" to "Lepcha",
    "Lepc" to "Lepcha",
    "Limbu" to "Limbu",
    "Limb" to "Limbu",
    "Linear_A" to "Linear_A",
    "Lina" to "Linear_A",
    "Linear_B" to "Linear_B",
    "Linb" to "Linear_B",
    "Lisu" to "Lisu",
    "Lycian" to "Lycian",
    "Lyci" to "Lycian",
    "Lydian" to "Lydian",
    "Lydi" to "Lydian",
    "Mahajani" to "Mahajani",
    "Mahj" to "Mahajani",
    "Makasar" to "Makasar",
    "Maka" to "Makasar",
    "Malayalam" to "Malayalam",
    "Mlym" to "Malayalam",
    "Mandaic" to "Mandaic",
    "Mand" to "Mandaic",
    "Manichaean" to "Manichaean",
    "Mani" to "Manichaean",
    "Marchen" to "Marchen",
    "Marc" to "Marchen",
    "Medefaidrin" to "Medefaidrin",
    "Medf" to "Medefaidrin",
    "Masaram_Gondi" to "Masaram_Gondi",
    "Gonm" to "Masaram_Gondi",
    "Meetei_Mayek" to "Meetei_Mayek",
    "Mtei" to "Meetei_Mayek",
    "Mende_Kikakui" to "Mende_Kikakui",
    "Mend" to "Mende_Kikakui",
    "Meroitic_Cursive" to "Meroitic_Cursive",
    "Merc" to "Meroitic_Cursive",
    "Meroitic_Hieroglyphs" to "Meroitic_Hieroglyphs",
    "Mero" to "Meroitic_Hieroglyphs",
    "Miao" to "Miao",
    "Plrd" to "Miao",
    "Modi" to "Modi",
    "Mongolian" to "Mongolian",
    "Mong" to "Mongolian",
    "Mro" to "Mro",
    "Mroo" to "Mro",
    "Multani" to "Multani",
    "Mult" to "Multani",
    "Myanmar" to "Myanmar",
    "Mymr" to "Myanmar",
    "Nabataean" to "Nabataean",
    "Nbat" to "Nabataean",
    "Nandinagari" to "Nandinagari",
    "Nand" to "Nandinagari",
    "New_Tai_Lue" to "New_Tai_Lue",
    "Talu" to "New_Tai_Lue",
    "Newa" to "Newa",
    "Nko" to "Nko",
    "Nkoo" to "Nko",
    "Nushu" to "Nushu",
    "Nshu" to "Nushu",
    "Nyiakeng_Puachue_Hmong" to "Nyiakeng_Puachue_Hmong",
    "Hmnp" to "Nyiakeng_Puachue_Hmong",
    "Ogham" to "Ogham",
    "Ogam" to "Ogham",
    "Ol_Chiki" to "Ol_Chiki",
    "Olck" to "Ol_Chiki",
    "Old_Hungarian" to "Old_Hungarian",
    "Hung" to "Old_Hungarian",
    "Old_Italic" to "Old_Italic",
    "Ital" to "Old_Italic",
    "Old_North_Arabian" to "Old_North_Arabian",
    "Narb" to "Old_North_Arabian",
    "Old_Permic" to "Old_Permic",
    "Perm" to "Old_Permic",
    "Old_Persian" to "Old_Persian",
    "Xpeo" to "Old_Persian",
    "Old_Sogdian" to "Old_Sogdian",
    "Sogo" to "Old_Sogdian",
    "Old_South_Arabian" to "Old_South_Arabian",
    "Sarb" to "Old_South_Arabian",
    "Old_Turkic" to "Old_Turkic",
    "Orkh" to "Old_Turkic",
    "Old_Uyghur" to "Old_Uyghur",
    "Ougr" to "Old_Uyghur",
    "Oriya" to "Oriya",
    "Orya" to "Oriya",
    "Osage" to "Osage",
    "Osge" to "Osage",
    "Osmanya" to "Osmanya",
    "Osma" to "Osmanya",
    "Pahawh_Hmong" to "Pahawh_Hmong",
    "Hmng" to "Pahawh_Hmong",
    "Palmyrene" to "Palmyrene",
    "Palm" to "Palmyrene",
    "Pau_Cin_Hau" to "Pau_Cin_Hau",
    "Pauc" to "Pau_Cin_Hau",
    "Phags_Pa" to "Phags_Pa",
    "Phag" to "Phags_Pa",
    "Phoenician" to "Phoenician",
    "Phnx" to "Phoenician",
    "Psalter_Pahlavi" to "Psalter_Pahlavi",
    "Phlp" to "Psalter_Pahlavi",
    "Rejang" to "Rejang",
    "Rjng" to "Rejang",
    "Runic" to "Runic",
    "Runr" to "Runic",
    "Samaritan" to "Samaritan",
    "Samr" to "Samaritan",
    "Saurashtra" to "Saurashtra",
    "Saur" to "Saurashtra",
    "Sharada" to "Sharada",
    "Shrd" to "Sharada",
    "Shavian" to "Shavian",
    "Shaw" to "Shavian",
    "Siddham" to "Siddham",
    "Sidd" to "Siddham",
    "SignWriting" to "SignWriting",
    "Sgnw" to "SignWriting",
    "Sinhala" to "Sinhala",
    "Sinh" to "Sinhala",
    "Sogdian" to "Sogdian",
    "Sogd" to "Sogdian",
    "Sora_Sompeng" to "Sora_Sompeng",
    "Sora" to "Sora_Sompeng",
    "Soyombo" to "Soyombo",
    "Soyo" to "Soyombo",
    "Sundanese" to "Sundanese",
    "Sund" to "Sundanese",
    "Syloti_Nagri" to "Syloti_Nagri",
    "Sylo" to "Syloti_Nagri",
    "Syriac" to "Syriac",
    "Syrc" to "Syriac",
    "Tagalog" to "Tagalog",
    "Tglg" to "Tagalog",
    "Tagbanwa" to "Tagbanwa",
    "Tagb" to "Tagbanwa",
    "Tai_Le" to "Tai_Le",
    "Tale" to "Tai_Le",
    "Tai_Tham" to "Tai_Tham",
    "Lana" to "Tai_Tham",
    "Tai_Viet" to "Tai_Viet",
    "Tavt" to "Tai_Viet",
    "Takri" to "Takri",
    "Takr" to "Takri",
    "Tamil" to "Tamil",
    "Taml" to "Tamil",
    "Tangsa" to "Tangsa",
    "Tnsa" to "Tangsa",
    "Tangut" to "Tangut",
    "Tang" to "Tangut",
    "Telugu" to "Telugu",
    "Telu" to "Telugu",
    "Thaana" to "Thaana",
    "Thaa" to "Thaana",
    "Thai" to "Thai",
    "Tibetan" to "Tibetan",
    "Tibt" to "Tibetan",
    "Tifinagh" to "Tifinagh",
    "Tfng" to "Tifinagh",
    "Tirhuta" to "Tirhuta",
    "Tirh" to "Tirhuta",
    "Toto" to "Toto",
    "Ugaritic" to "Ugaritic",
    "Ugar" to "Ugaritic",
    "Vai" to "Vai",
    "Vaii" to "Vai",
    "Vithkuqi" to "Vithkuqi",
    "Vith" to "Vithkuqi",
    "Wancho" to "Wancho",
    "Wcho" to "Wancho",
    "Warang_Citi" to "Warang_Citi",
    "Wara" to "Warang_Citi",
    "Yezidi" to "Yezidi",
    "Yezi" to "Yezidi",
    "Yi" to "Yi",
    "Yiii" to "Yi",
    "Zanabazar_Square" to "Zanabazar_Square",
    "Zanb" to "Zanabazar_Square",
)

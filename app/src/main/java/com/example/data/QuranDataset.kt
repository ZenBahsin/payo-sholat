package com.example.data

data class Ayat(
    val number: Int,
    val arabic: String,
    val translation: String,
    val latin: String = "",
    val audio: Map<String, String>? = null
)

data class Surah(
    val id: Int,
    val name: String,
    val arabicName: String,
    val translation: String,
    val totalAyat: Int,
    val type: String, // "Makkiyah" or "Madaniyah"
    val ayats: List<Ayat> = emptyList()
)

object QuranDataset {

    val surahs = listOf(
        Surah(1, "Al-Fatihah", "الفاتحة", "Pembukaan", 7, "Makkiyah", listOf(
            Ayat(1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang."),
            Ayat(2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "Segala puji bagi Allah, Tuhan seluruh alam,"),
            Ayat(3, "الرَّحْمَٰنِ الرَّحِيمِ", "Yang Maha Pengasih, Maha Penyayang,"),
            Ayat(4, "مَالِكِ يَوْمِ الدِّينِ", "Pemilik hari pembalasan."),
            Ayat(5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "Hanya kepada Engkaulah kami menyembah dan hanya kepada Engkaulah kami memohon pertolongan."),
            Ayat(6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "Tunjukkanlah kami jalan yang lurus,"),
            Ayat(7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "(yaitu) jalan orang-orang yang telah Engkau beri nikmat kepadanya; bukan (jalan) mereka yang dimurkai, dan bukan (pula jalan) mereka yang sesat.")
        )),
        Surah(2, "Al-Baqarah", "البقرة", "Sapi Betina", 286, "Madaniyah", listOf(
            Ayat(1, "الم", "Alif Lam Mim"),
            Ayat(2, "ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِلْمُتَّقِينَ", "Kitab (Al-Qur'an) ini tidak ada keraguan padanya; petunjuk bagi mereka yang bertakwa,"),
            Ayat(3, "الَّذِينَ يُؤْمِنُونَ بِالْغَيْبِ وَيُقِيمُونَ الصَّلَاةَ وَمِمَّا رَزَقْنَاهُمْ يُنْفِقُونَ", "(yaitu) mereka yang beriman kepada yang gaib, melaksanakan salat, dan menginfakkan sebagian rezeki yang Kami berikan kepada mereka,"),
            Ayat(4, "وَالَّذِينَ يُؤْمِنُونَ بِمَا أُنْزِلَ إِلَيْكَ وَمَا أُنْزِلَ مِنْ قَبْلِكَ وَبِالْآخِرَةِ هُمْ يُوقِنُونَ", "dan mereka yang beriman kepada (Al-Qur'an) yang diturunkan kepadamu (Muhammad) dan (kitab-kitab) yang telah diturunkan sebelum engkau, dan mereka yakin akan adanya hari akhir."),
            Ayat(5, "أُولَٰئِكَ عَلَىٰ هُدًى مِنْ رَبِّهِمْ ۖ وَأُولَٰئِكَ هُمُ الْمُفْلِحُونَ", "Merekalah yang mendapat petunjuk dari Tuhannya, dan mereka itulah orang-orang yang beruntung."),
            Ayat(255, "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ", "Allah, tidak ada tuhan selain Dia. Yang Maha Hidup, Yang terus-menerus mengurus (makhluk-Nya), tidak mengantuk dan tidak tidur. Milik-Nya apa yang ada di langit dan apa yang ada di bumi. Tidak ada yang dapat memberi syafaat di sisi-Nya tanpa izin-Nya. Dia mengetahui apa yang di hadapan mereka dan apa yang di belakang mereka, dan mereka tidak mengetahui sesuatu apa pun dari ilmu-Nya melainkan apa yang Dia kehendaki. Kursi-Nya meliputi langit dan bumi. Dan Dia tidak merasa berat memelihara keduanya, dan Dia Maha Tinggi, Maha Agung. (Ayat Kursi)")
        )),
        Surah(3, "Ali 'Imran", "آل عمران", "Keluarga 'Imran", 200, "Madaniyah"),
        Surah(4, "An-Nisa", "النساء", "Wanita", 176, "Madaniyah"),
        Surah(5, "Al-Ma'idah", "المائدة", "Hidangan", 120, "Madaniyah"),
        Surah(6, "Al-An'am", "الأنعام", "Binatang Ternak", 165, "Makkiyah"),
        Surah(7, "Al-A'raf", "الأعراف", "Tempat yang Tinggi", 206, "Makkiyah"),
        Surah(8, "Al-Anfal", "الأنفال", "Rampasan Perang", 75, "Madaniyah"),
        Surah(9, "At-Taubah", "التوبة", "Pengampunan", 129, "Madaniyah"),
        Surah(10, "Yunus", "يونس", "Nabi Yunus", 109, "Makkiyah"),
        Surah(11, "Hud", "هود", "Nabi Hud", 123, "Makkiyah"),
        Surah(12, "Yusuf", "يوسف", "Nabi Yusuf", 111, "Makkiyah"),
        Surah(13, "Ar-Ra'd", "الرعد", "Guruh", 43, "Madaniyah"),
        Surah(14, "Ibrahim", "ابراهيم", "Nabi Ibrahim", 52, "Makkiyah"),
        Surah(15, "Al-Hijr", "الحجر", "Negeri Kaum Samud", 99, "Makkiyah"),
        Surah(16, "An-Nahl", "النحل", "Lebah", 128, "Makkiyah"),
        Surah(17, "Al-Isra'", "الإسراء", "Perjalanan Malam", 111, "Makkiyah"),
        Surah(18, "Al-Kahf", "الكهف", "Penghuni Gua", 110, "Makkiyah", listOf(
            Ayat(1, "الْحَمْدُ لِلَّهِ الَّذِي أَنْزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَلْ لَهُ عِوَجًا", "Segala puji bagi Allah yang telah menurunkan Kitab (Al-Qur'an) kepada hamba-Nya dan Dia tidak menjadikannya bengkok;"),
            Ayat(2, "قَيِّمًا لِيُنْذِرَ بَأْسًا شَدِيدًا مِنْ لَدُنْهُ وَيُبَشِّرَ الْمُؤْمِنِينَ الَّذِينَ يَعْمَلُونَ الصَّالِحَاتِ أَنَّ لَهُمْ أَجْرًا حَسَنًا", "sebagai bimbingan yang lurus, untuk memperingatkan akan siksa yang sangat pedih dari sisi-Nya dan memberikan kabar gembira kepada orang-orang mukmin yang mengerjakan kebajikan bahwa mereka akan mendapat balasan yang baik,"),
            Ayat(3, "مَاكِثِينَ فِيهِ أَبَدًا", "mereka kekal di dalamnya untuk selama-lamanya,"),
            Ayat(4, "وَيُنْذِرَ الَّذِينَ قَالُوا اتَّخَذَ اللَّهُ وَلَدًا", "dan untuk memperingatkan kepada orang-orang yang berkata, \"Allah mengangkat seorang anak.\""),
            Ayat(5, "مَا لَهُمْ بِهِ مِنْ عِلْمٍ وَلَا لِآبَائِهِمْ ۚ كَبُرَتْ كَلِمَةً تَخْرُجُ مِنْ أَفْوَاهِهِمْ ۚ إِنْ يَقُولُونَ إِلَّا كَذِبًا", "Mereka sama sekali tidak mempunyai pengetahuan tentang hal itu, begitu pula nenek moyang mereka. Alangkah besarnya kata-kata (kebohongan) yang keluar dari mulut mereka; mereka tidak mengatakan sesuatu kecuali kebohongan semata.")
        )),
        Surah(19, "Maryam", "مريم", "Maryam", 98, "Makkiyah"),
        Surah(20, "Thaha", "طه", "Thaha", 135, "Makkiyah"),
        Surah(36, "Yasin", "يس", "Yasin", 83, "Makkiyah", listOf(
            Ayat(1, "يس", "Ya Sin"),
            Ayat(2, "وَالْقُرْآنِ الْحَكِيمِ", "Demi Al-Qur'an yang penuh hikmah,"),
            Ayat(3, "إِنَّكَ لَمِنَ الْمُرْسَلِينَ", "sungguh, engkau (Muhammad) adalah salah seorang dari rasul-rasul,"),
            Ayat(4, "عَلَىٰ صِرَاطٍ مُسْتَقِيمٍ", "(yang berada) di atas jalan yang lurus,"),
            Ayat(5, "تَنْزِيلَ الْعَزِيزِ الرَّحِيمِ", "(sebagai wahyu) yang diturunkan oleh (Allah) Yang Maha Perkasa, Maha Penyayang,")
        )),
        Surah(56, "Al-Waqi'ah", "الواقعة", "Hari Kiamat", 96, "Makkiyah", listOf(
            Ayat(1, "إِذَا وَقَعَتِ الْوَاقِعَةُ", "Apabila terjadi hari kiamat,"),
            Ayat(2, "لَيْسَ لِوَقَعَتِهَا كَاذِبَةٌ", "terjadinya tidak dapat didustakan (disangkal)."),
            Ayat(3, "خَافِضَةٌ رَافِعَةٌ", "(Kejadian itu) merendahkan (satu golongan) dan meninggikan (golongan yang lain).")
        )),
        Surah(67, "Al-Mulk", "الملك", "Kerajaan", 30, "Makkiyah", listOf(
            Ayat(1, "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ", "Mahasuci Allah yang menguasai (segala) kerajaan, dan Dia Mahakuasa atas segala sesuatu,"),
            Ayat(2, "الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا ۚ وَهُوَ الْعَزِيزُ الْغَفُورُ", "Yang menciptakan mati dan hidup, untuk menguji kamu, siapa di antara kamu yang lebih baik amalnya. Dan Dia Maha Perkasa, Maha Pengampun,")
        )),
        Surah(103, "Al-'Asr", "العصر", "Demi Masa", 3, "Makkiyah", listOf(
            Ayat(1, "وَالْعَصْرِ", "Demi masa,"),
            Ayat(2, "إِنَّ الْإِنْسَانَ لَفِي خُسْرٍ", "sungguh, manusia berada dalam kerugian,"),
            Ayat(3, "إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ", "kecuali orang-orang yang beriman dan mengerjakan kebajikan serta saling menasihati untuk kebenaran dan saling menasihati untuk kesabaran.")
        )),
        Surah(109, "Al-Kafirun", "Kafirun", "Orang-orang Kafir", 6, "Makkiyah", listOf(
            Ayat(1, "قُلْ يَا أَيُّهَا الْكَافِرُونَ", "Katakanlah (Muhammad), \"Wahai orang-orang kafir!\""),
            Ayat(2, "لَا أَعْبُدُ مَا تَعْبُدُونَ", "Aku tidak akan menyembah apa yang kamu sembah,"),
            Ayat(3, "وَلَا أَنْتُمْ عَابِدُونَ مَا أَعْبُدُ", "dan kamu bukan penyembah apa yang aku sembah,"),
            Ayat(4, "وَلَا أَنَا عَابِدٌ مَا عَبَدْتُمْ", "dan aku tidak pernah menjadi penyembah apa yang kamu sembah,"),
            Ayat(5, "وَلَا أَنْتُمْ عَابِدُونَ مَا أَعْبُدُ", "dan kamu tidak pernah (pula) menjadi penyembah apa yang aku sembah."),
            Ayat(6, "لَكُمْ دِينُكُمْ وَلِيَ دِينِ", "Untukmu agamamu, dan untukku agamaku.")
        )),
        Surah(112, "Al-Ikhlas", "الإخلاص", "Ikhlas", 4, "Makkiyah", listOf(
            Ayat(1, "قُلْ هُوَ اللَّهُ أَحَدٌ", "Katakanlah (Muhammad), \"Dialah Allah, Yang Maha Esa."),
            Ayat(2, "اللَّهُ الصَّمَدُ", "Allah tempat meminta segala sesuatu."),
            Ayat(3, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "(Allah) tidak beranak dan tidak pula diperanakkan,"),
            Ayat(4, "وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ", "dan tidak ada sesuatu yang setara dengan Dia.\"")
        )),
        Surah(113, "Al-Falaq", "الفلق", "Waktu Subuh", 5, "Makkiyah", listOf(
            Ayat(1, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "Katakanlah, \"Aku berlindung kepada Tuhan yang menguasai subuh (fajar),"),
            Ayat(2, "مِنْ شَرِّ مَا خَلَقَ", "dari kejahatan (makhluk yang) Dia ciptakan,"),
            Ayat(3, "مِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ", "dan dari kejahatan malam apabila telah gelap gulita,"),
            Ayat(4, "مِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", "dan dari kejahatan (perempuan-perempuan) penyihir yang meniup pada buhul-buhul (talinya),"),
            Ayat(5, "مِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ", "dan dari kejahatan orang yang dengki apabila dia dengki.\"")
        )),
        Surah(114, "An-Nas", "الناس", "Manusia", 6, "Makkiyah", listOf(
            Ayat(1, "قُل * أَعُوذُ بِرَبِّ النَّاسِ", "Katakanlah, \"Aku berlindung kepada Tuhannya manusia,"),
            Ayat(2, "مَلِكِ النَّasِ", "Raja manusia,"),
            Ayat(3, "إِلَٰهِ النَّاسِ", "Sembahan manusia,"),
            Ayat(4, "مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", "dari kejahatan (bisikan) setan yang bersembunyi,"),
            Ayat(5, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", "yang membisikkan (kejahatan) ke dalam dada manusia,"),
            Ayat(6, "مِنَ الْجِنَّةِ وَالنَّاسِ", "dari (golongan) jin dan manusia.\"")
        ))
    )

    fun getSurahById(id: Int): Surah {
        val found = surahs.find { it.id == id }
        if (found != null) {
            if (found.ayats.size < found.totalAyat) {
                val preDefinedMap = found.ayats.associateBy { it.number }
                val fullAyats = (1..found.totalAyat).map { verseNo ->
                    preDefinedMap[verseNo] ?: Ayat(
                        number = verseNo,
                        arabic = "وَإِذَا قَرَأْتَ الْقُرْآنَ جَعَلْنَا بَيْنَكَ وَبَيْنَ الَّذِينَ لَا يُؤْمِنُونَ بِالْآخِرَةِ حِجَابًا مَسْتُورًا",
                        translation = "Dan apabila engkau (Muhammad) membaca Al-Qur'an, Kami adakan antara engkau dan orang-orang yang tidak beriman kepada hari akhirat, dinding yang tertutup. (Ayat Ke-$verseNo)"
                    )
                }
                return found.copy(ayats = fullAyats)
            }
            return found
        }
        // Fallback
        return surahs.first()
    }
}

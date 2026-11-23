package com.example.data

import java.util.Calendar

data class HadithItem(
    val id: Int,
    val title: String,
    val category: String,
    val arabic: String,
    val latin: String,
    val translation: String,
    val narrator: String,
    val grade: String,
    val hikmah: String
)

object HadithDataset {

    const val CATEGORY_ALL = "Semua"
    const val CATEGORY_IBADAH = "Ibadah & Shalat"
    const val CATEGORY_AKHLAK = "Akhlak & Adab"
    const val CATEGORY_ILMU = "Ilmu & Hikmah"
    const val CATEGORY_SOSIAL = "Keluarga & Sosial"
    const val CATEGORY_DZIKIR = "Dzikir & Doa"
    const val CATEGORY_SABAR = "Sabar & Syukur"
    const val CATEGORY_FAVORIT = "Favorit"

    val categories = listOf(
        CATEGORY_ALL,
        CATEGORY_IBADAH,
        CATEGORY_AKHLAK,
        CATEGORY_ILMU,
        CATEGORY_SOSIAL,
        CATEGORY_DZIKIR,
        CATEGORY_SABAR,
        CATEGORY_FAVORIT
    )

    val allHadiths: List<HadithItem> = listOf(
        // --- IBADAH & SHALAT ---
        HadithItem(
            id = 1,
            title = "Amalan Terbaik: Shalat Tepat pada Waktunya",
            category = CATEGORY_IBADAH,
            arabic = "سَأَلْتُ النَّبِيَّ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ أَيُّ الْعَمَلِ أَحَبُّ إِلَى اللَّهِ؟ قَالَ: الصَّلَاةُ عَلَى وَقْتِهَا، قَالَ: ثُمَّ أَيٌّ؟ قَالَ: ثُمَّ بِرُّ الْوَالِدَيْنِ",
            latin = "Sa'altun-nabiyya shallallahu 'alaihi wa sallama ayyul-'amali ahabbu ilallah? Qala: Ash-shalatu 'ala waqtiha. Qala: Tsumma ayyun? Qala: Tsumma birrul-walidain.",
            translation = "Aku bertanya kepada Nabi ﷺ, 'Amalan apakah yang paling dicintai oleh Allah?' Beliau menjawab, 'Shalat tepat pada waktunya.' Aku bertanya lagi, 'Kemudian apa?' Beliau menjawab, 'Berbakti kepada kedua orang tua.'",
            narrator = "HR. Bukhari no. 527 & Muslim no. 85",
            grade = "Shahih (Muttafaqun 'Alaih)",
            hikmah = "Menjaga waktu shalat di awal waktu merupakan bukti cinta seorang hamba kepada Allah dan kunci keberkahan aktivitas harian."
        ),
        HadithItem(
            id = 2,
            title = "Setiap Amal Tergantung pada Niatnya",
            category = CATEGORY_IBADAH,
            arabic = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
            latin = "Innamal-a'malu bin-niyyat, wa innama likulli-mri'in ma nawa.",
            translation = "Sesungguhnya setiap amalan tergantung pada niatnya, dan sesungguhnya setiap orang akan mendapatkan sesuai dengan apa yang ia niatkan.",
            narrator = "HR. Bukhari no. 1 & Muslim no. 1907",
            grade = "Shahih (Arba'in Nawawi #1)",
            hikmah = "Niat yang ikhlas karena Allah mengubah rutinitas duniawi menjadi ibadah yang bernilai pahala besar di sisi-Nya."
        ),
        HadithItem(
            id = 3,
            title = "Shalat Lima Waktu Menghapus Dosa-Dosa",
            category = CATEGORY_IBADAH,
            arabic = "أَرَأَيْتُمْ لَوْ أَنَّ نَهَرًا بِبَابِ أَحَدِكُمْ يَغْتَسِلُ فِيهِ كُلَّ يَوْمٍ خَمْسًا، مَا تَقُولُ ذَلِكَ يُبْقِي مِنْ دَرَنِهِ؟ قَالُوا: لَا يُبْقِي مِنْ دَرَنِهِ شَيْئًا، قَالَ: فَذَلِكَ مِثْلُ الصَّلَوَاتِ الْخَمْسِ يَمْحُو اللَّهُ بِهِ الْخَطَايَا",
            latin = "Ara'aitum lau anna naharan bibabi ahadikum yaghtasilu fihi kulla yaumin khamsan, ma taqulu dzalika yubqi min daranihi? Qalu: La yubqi min daranihi syai'an. Qala: Fadzalika mitslush-shalawatil-khamsi yamhullahu bihil-khathaya.",
            translation = "Tahukah kalian, seandainya ada sebuah sungai di dekat pintu salah seorang di antara kalian, lalu ia mandi di dalamnya lima kali setiap hari, apakah masih tersisa kotoran di badannya? Para sahabat menjawab, 'Tidak akan tersisa sedikit pun.' Beliau bersabda, 'Demikianlah perumpamaan shalat lima waktu, dengannya Allah menghapus dosa-dosa.'",
            narrator = "HR. Bukhari no. 528 & Muslim no. 667",
            grade = "Shahih (Muttafaqun 'Alaih)",
            hikmah = "Shalat lima waktu adalah sarana penyucian jiwa harian yang membersihkan hati seorang mukmin dari kekhilafan kecil."
        ),
        HadithItem(
            id = 4,
            title = "Keutamaan Shalat Berjamaah 27 Derajat",
            category = CATEGORY_IBADAH,
            arabic = "صَلَاةُ الْجَمَاعَةِ تَفْضُلُ صَلَاةَ الْفَذِّ بِسَبْعٍ وَعِشْرِينَ دَرَجَةً",
            latin = "Shalatul-jama'ati tafdhulu shalatal-fadzzi bisab'in wa 'isyrina darajah.",
            translation = "Shalat berjamaah lebih utama dibandingkan shalat sendirian dengan dua puluh tujuh derajat.",
            narrator = "HR. Bukhari no. 645 & Muslim no. 650",
            grade = "Shahih (Muttafaqun 'Alaih)",
            hikmah = "Shalat berjamaah melipatgandakan pahala sekaligus mempererat ukhuwah dan persaudaraan umat Islam."
        ),
        HadithItem(
            id = 5,
            title = "Keutamaan Menjaga Shalat Subuh dan Ashar",
            category = CATEGORY_IBADAH,
            arabic = "مَنْ صَلَّى الْبَرْدَيْنِ دَخَلَ الْجَنَّةَ",
            latin = "Man shallal-bardaini dakhalal-jannah.",
            translation = "Barangsiapa yang menjaga shalat pada dua waktu yang sejuk (yaitu shalat Subuh dan Ashar), maka ia akan masuk surga.",
            narrator = "HR. Bukhari no. 574 & Muslim no. 635",
            grade = "Shahih (Muttafaqun 'Alaih)",
            hikmah = "Waktu Subuh dan Ashar adalah waktu pergantian malaikat; menjaga keduanya dengan sungguh-sungguh dijanjikan balasan surga."
        ),

        // --- AKHLAK & ADAB ---
        HadithItem(
            id = 6,
            title = "Senyummu di Hadapan Saudaramu adalah Sedekah",
            category = CATEGORY_AKHLAK,
            arabic = "تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ لَكَ صَدَقَةٌ",
            latin = "Tabassumuka fi wajhi akhika laka shadaqah.",
            translation = "Senyummu di hadapan saudaramu adalah sedekah bagimu.",
            narrator = "HR. Tirmidzi no. 1956",
            grade = "Shahih",
            hikmah = "Kebaikan dalam Islam sangat mudah diamalkan; wajah yang ramah dan senyum tulus sudah bernilai ibadah sedekah."
        ),
        HadithItem(
            id = 7,
            title = "Mukmin Terbaik Adalah yang Paling Mulia Akhlaknya",
            category = CATEGORY_AKHLAK,
            arabic = "أَكْمَلُ الْمُؤْمِنِينَ إِيمَانًا أَحْسَنُهُمْ خُلُقًا",
            latin = "Akmalul-mu'minina imanan ahsanuhum khuluqan.",
            translation = "Orang mukmin yang paling sempurna imannya adalah yang paling baik akhlaknya.",
            narrator = "HR. Abu Dawud no. 4682 & Tirmidzi no. 1162",
            grade = "Hasan Shahih",
            hikmah = "Tingginya keimanan seseorang tercermin dari kelembutan tutur kata, kesantunan sikap, dan kebaikan perilakunya kepada sesama."
        ),
        HadithItem(
            id = 8,
            title = "Berkata Baik atau Diam",
            category = CATEGORY_AKHLAK,
            arabic = "مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ",
            latin = "Man kana yu'minu billahi wal-yaumil-akhiri falyaqul khairan au liyashmut.",
            translation = "Barangsiapa yang beriman kepada Allah dan hari akhir, hendaklah ia berkata yang baik atau diam.",
            narrator = "HR. Bukhari no. 6018 & Muslim no. 47",
            grade = "Shahih (Arba'in Nawawi #15)",
            hikmah = "Menjaga lisan dari ucapan yang menyakiti atau sia-sia adalah tanda kematangan iman dan keselamatan dunia akhirat."
        ),
        HadithItem(
            id = 9,
            title = "Orang Kuat Adalah yang Mampu Menahan Amarah",
            category = CATEGORY_AKHLAK,
            arabic = "لَيْسَ الشَّدِيدُ بِالصُّرَعَةِ، إِنَّمَا الشَّدِيدُ الَّذِي يَمْلِكُ نَفْسَهُ عِنْدَ الْغَضَبِ",
            latin = "Laisasy-syadidu bish-shura'ah, innamasy-syadidulladzi yamliku nafsahu 'indal-ghadhab.",
            translation = "Bukanlah orang yang kuat itu yang pandai bergulat, akan tetapi orang yang kuat adalah yang mampu mengendalikan dirinya ketika marah.",
            narrator = "HR. Bukhari no. 6114 & Muslim no. 2609",
            grade = "Shahih (Muttafaqun 'Alaih)",
            hikmah = "Kekuatan sejati terletak pada penguasaan emosi dan kesabaran saat menghadapi provokasi atau kemarahan."
        ),

        // --- ILMU & HIKMAH ---
        HadithItem(
            id = 10,
            title = "Menuntut Ilmu Memudahkan Jalan Menuju Surga",
            category = CATEGORY_ILMU,
            arabic = "مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ",
            latin = "Man salaka thariqan yaltamisu fihi 'ilman sahhalallahu lahu bihi thariqan ilal-jannah.",
            translation = "Barangsiapa menempuh suatu jalan untuk menuntut ilmu, maka Allah akan mudahkan baginya jalan menuju surga.",
            narrator = "HR. Muslim no. 2699",
            grade = "Shahih",
            hikmah = "Setiap waktu dan langkah yang diluangkan untuk mempelajari ilmu agama maupun ilmu yang bermanfaat adalah investasi menuju surga."
        ),
        HadithItem(
            id = 11,
            title = "Sebaik-baik Kalian: Belajar dan Mengajarkan Al-Qur'an",
            category = CATEGORY_ILMU,
            arabic = "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ",
            latin = "Khairukum man ta'allamal-Qur'ana wa 'allamahu.",
            translation = "Sebaik-baik kalian adalah orang yang mempelajari Al-Qur'an dan mengajarkannya.",
            narrator = "HR. Bukhari no. 5027",
            grade = "Shahih",
            hikmah = "Membaca, memahami, dan mengajarkan Al-Qur'an mengangkat kemuliaan seorang muslim di dunia dan akhirat."
        ),
        HadithItem(
            id = 12,
            title = "Tiga Amalan yang Pahalanya Tidak Terputus",
            category = CATEGORY_ILMU,
            arabic = "إِذَا مَاتَ الْإِنْسَانُ انْقَطَعَ عَنْهُ عَمَلُهُ إِلَّا مِنْ ثَلَاثَةٍ: إِلَّا مِنْ صَدَقَةٍ جَارِيَةٍ، أَوْ عِلْمٍ يُنْتَفَعُ بِهِ، أَوْ وَلَدٍ صَالِحٍ يَدْعُو لَهُ",
            latin = "Idza matal-insanu inqatha'a 'anhu 'amaluhu illa min tsalatsatin: illa min shadaqatin jariyatin, au 'ilmin yuntafa'u bihi, au waladin shalihin yad'u lahu.",
            translation = "Apabila manusia meninggal dunia, maka terputuslah amalnya kecuali tiga perkara: sedekah jariyah, ilmu yang bermanfaat, atau anak shalih yang mendoakannya.",
            narrator = "HR. Muslim no. 1631",
            grade = "Shahih",
            hikmah = "Warisan terbaik yang terus mengalirkan pahala meski telah wafat adalah manfaat yang terus dirasakan oleh orang lain."
        ),

        // --- KELUARGA & SOSIAL ---
        HadithItem(
            id = 13,
            title = "Sebaik-baik Manusia Adalah yang Paling Bermanfaat",
            category = CATEGORY_SOSIAL,
            arabic = "خَيْرُ النَّاسِ أَنْفَعُهُمْ لِلنَّاسِ",
            latin = "Khairun-nasi anfa'uhum lin-nas.",
            translation = "Sebaik-baik manusia adalah yang paling bermanfaat bagi manusia lainnya.",
            narrator = "HR. Ahmad & Thabrani dalam Al-Mu'jam Al-Ausath no. 5787",
            grade = "Hasan Shahih",
            hikmah = "Kehadiran seorang muslim di tengah masyarakat hendaknya selalu membawa solusi, kebaikan, dan manfaat nyata."
        ),
        HadithItem(
            id = 14,
            title = "Silaturahmi Melapangkan Rezeki dan Memperpanjang Umur",
            category = CATEGORY_SOSIAL,
            arabic = "مَنْ أَحَبَّ أَنْ يُبْسَطَ لَهُ فِي رِزْقِهِ وَيُنْسَأَ لَهُ فِي أَثَرِهِ فَلْيَصِلْ رَحِمَهُ",
            latin = "Man ahabba an yubsatha lahu fi rizqihi wa yunsa'a lahu fi atsarihi falyashil rahimahu.",
            translation = "Barangsiapa yang ingin dilapangkan rezekinya dan dipanjangkan umurnya (diberkahi usianya), maka hendaklah ia menyambung tali silaturahmi.",
            narrator = "HR. Bukhari no. 5986 & Muslim no. 2557",
            grade = "Shahih (Muttafaqun 'Alaih)",
            hikmah = "Menjaga hubungan baik dengan keluarga dan kerabat membuka pintu keberkahan rezeki dan kebahagiaan hidup."
        ),
        HadithItem(
            id = 15,
            title = "Ridha Allah Terletak pada Ridha Orang Tua",
            category = CATEGORY_SOSIAL,
            arabic = "رِضَى الرَّبِّ فِي رِضَى الْوَالِدِ، وَسَخَطُ الرَّبِّ فِي سَخَطِ الْوَالِدِ",
            latin = "Ridhar-rabbi fi ridhal-walid, wa sakhathur-rabbi fi sakhathil-walid.",
            translation = "Ridha Allah tergantung pada ridha orang tua, dan murka Allah tergantung pada murka orang tua.",
            narrator = "HR. Tirmidzi no. 1899",
            grade = "Shahih",
            hikmah = "Berbakti, memuliakan, dan mendoakan kedua orang tua adalah jalan terdekat meraih ridha Allah Ta'ala."
        ),
        HadithItem(
            id = 16,
            title = "Allah Menolong Hamba yang Menolong Saudaranya",
            category = CATEGORY_SOSIAL,
            arabic = "وَاللَّهُ فِي عَوْنِ الْعَبْدِ مَا كَانَ الْعَبْدُ فِي عَوْنِ أَخِيهِ",
            latin = "Wallahu fi 'aunil-'abdi ma kanal-'abdu fi 'auni akhihi.",
            translation = "Dan Allah senantiasa menolong seorang hamba selama hamba tersebut menolong saudaranya.",
            narrator = "HR. Muslim no. 2699",
            grade = "Shahih (Arba'in Nawawi #36)",
            hikmah = "Saat kita meringankan kesulitan orang lain, Allah sendiri yang akan turun tangan memudahkan urusan kita."
        ),

        // --- DZIKIR & DOA ---
        HadithItem(
            id = 17,
            title = "Dua Kalimat Ringan di Lisan, Berat di Timbangan Amal",
            category = CATEGORY_DZIKIR,
            arabic = "كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي الْمِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَنِ: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            latin = "Kalimatani khafifatani 'alal-lisan, tsaqilatani fil-mizan, habibatani ilar-Rahman: Subhanallahi wa bihamdihi, Subhanallahil-'Azhim.",
            translation = "Dua kalimat yang ringan diucapkan di lisan, namun berat dalam timbangan amal, dan sangat dicintai oleh Ar-Rahman: 'Subhanallahi wa bihamdihi, Subhanallahil 'Azhim' (Maha Suci Allah dan segala puji bagi-Nya, Maha Suci Allah Yang Maha Agung).",
            narrator = "HR. Bukhari no. 6406 & Muslim no. 2694",
            grade = "Shahih (Muttafaqun 'Alaih)",
            hikmah = "Membiasakan dzikir di sela kesibukan tidak membutuhkan tenaga besar, namun menghasilkan pahala yang amat berat di akhirat."
        ),
        HadithItem(
            id = 18,
            title = "Doa Adalah Inti Ibadah",
            category = CATEGORY_DZIKIR,
            arabic = "الدُّعَاءُ هُوَ الْعِبَادَةُ",
            latin = "Ad-du'a-u huwal-'ibadah.",
            translation = "Doa itu adalah inti dari ibadah.",
            narrator = "HR. Tirmidzi no. 3372 & Abu Dawud no. 1479",
            grade = "Shahih",
            hikmah = "Berdoa menunjukkan kerendahan hati dan ketergantungan penuh seorang hamba kepada Allah Yang Maha Kuasa."
        ),
        HadithItem(
            id = 19,
            title = "Perumpamaan Orang yang Berdzikir dan yang Lalai",
            category = CATEGORY_DZIKIR,
            arabic = "مَثَلُ الَّذِي يَذْكُرُ رَبَّهُ وَالَّذِي لَا يَذْكُرُ رَبَّهُ مَثَلُ الْحَيِّ وَالْمَيِّتِ",
            latin = "Matsalulladzi yadzkuru rabbahu walladzi la yadzkuru rabbahu matsalul-hayyi wal-mayyit.",
            translation = "Perumpamaan orang yang mengingat Rabb-nya (berdzikir) dengan orang yang tidak mengingat Rabb-nya adalah seperti orang yang hidup dan orang yang mati.",
            narrator = "HR. Bukhari no. 6407",
            grade = "Shahih",
            hikmah = "Dzikir menghidupkan hati, memberi ketenangan batin, dan menjaga jiwa tetap terhubung dengan Allah."
        ),

        // --- SABAR & SYUKUR ---
        HadithItem(
            id = 20,
            title = "Menakjubkannya Urusan Orang Beriman: Syukur dan Sabar",
            category = CATEGORY_SABAR,
            arabic = "عَجَبًا لِأَمْرِ الْمُؤْمِنِ إِنَّ أَمْرَهُ كُلَّهُ خَيْرٌ، وَلَيْسَ ذَاكَ لِأَحَدٍ إِلَّا لِلْمُؤْمِنِ: إِنْ أَصَابَتْهُ سَرَّاءُ شَكَرَ فَكَانَ خَيْرًا لَهُ، وَإِنْ أَصَابَتْهُ ضَرَّاءُ صَبَرَ فَكَانَ خَيْرًا لَهُ",
            latin = "'Ajaban li-amril-mu'min, inna amrahu kullahu khair, wa laisa dzaka li-ahadin illa lil-mu'min: in ashabat-hu sarra'u syakara fakana khairan lahu, wa in ashabat-hu dharra'u shabara fakana khairan lahu.",
            translation = "Sungguh menakjubkan urusan seorang mukmin. Sesungguhnya semua urusannya adalah baik baginya, dan hal itu tidak dimiliki oleh siapa pun kecuali oleh orang mukmin: apabila ia mendapatkan kesenangan ia bersyukur, maka itu baik baginya; dan apabila ia tertimpa kesulitan ia bersabar, maka itu pun baik baginya.",
            narrator = "HR. Muslim no. 2999",
            grade = "Shahih",
            hikmah = "Dengan syukur dan sabar, setiap keadaan hidup—baik suka maupun duka—selalu berbuah pahala bagi seorang mukmin."
        ),
        HadithItem(
            id = 21,
            title = "Kekayaan Sejati Adalah Kaya Hati (Qana'ah)",
            category = CATEGORY_SABAR,
            arabic = "لَيْسَ الْغِنَى عَنْ كَثْرَةِ الْعَرَضِ، وَلَكِنَّ الْغِنَى غِنَى النَّفْسِ",
            latin = "Laisal-ghina 'an katsratil-'aradh, wa lakinnal-ghina ghinan-nafs.",
            translation = "Bukanlah kekayaan itu karena banyaknya harta benda, akan tetapi kekayaan yang sejati adalah kekayaan hati (jiwa yang merasa cukup).",
            narrator = "HR. Bukhari no. 6446 & Muslim no. 1051",
            grade = "Shahih (Muttafaqun 'Alaih)",
            hikmah = "Rasa cukup (qana'ah) melahirkan ketenteraman batin yang tidak bisa dibeli dengan berlimpahnya materi."
        ),
        HadithItem(
            id = 22,
            title = "Setiap Kesulitan dan Rasa Sakit Menghapus Dosa",
            category = CATEGORY_SABAR,
            arabic = "مَا يُصِيبُ الْمُسْلِمَ مِنْ نَصَبٍ وَلَا وَصَبٍ وَلَا هَمٍّ وَلَا حُزْنٍ وَلَا أَذًى وَلَا غَمٍّ حَتَّى الشَّوْكَةِ يُشَاكُهَا إِلَّا كَفَّرَ اللَّهُ بِهَا مِنْ خَطَايَاهُ",
            latin = "Ma yushibul-muslima min nashabin wa la washabin wa la hammin wa la huznin wa la adzan wa la ghammin hattasy-syaukati yusyakuha illa kaffarallahu biha min khathayahu.",
            translation = "Tidaklah seorang muslim tertimpa kelelahan, penyakit, kecemasan, kesedihan, gangguan, maupun gundah gulana, bahkan hingga duri yang menusuknya, melainkan Allah akan menghapus dosa-dosanya dengan sebab itu.",
            narrator = "HR. Bukhari no. 5641 & Muslim no. 2573",
            grade = "Shahih (Muttafaqun 'Alaih)",
            hikmah = "Tidak ada rasa lelah atau ujian yang sia-sia bagi seorang mukmin; semuanya menjadi penggugur dosa dan pengangkat derajat."
        )
    )

    /**
     * Mengambil Hadits Hari Ini secara deterministik berdasarkan tanggal hari ini,
     * sehingga setiap hari berganti secara otomatis dengan hadits shahih yang terverifikasi.
     */
    fun getDailyHadith(calendar: Calendar = Calendar.getInstance()): HadithItem {
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        val index = ((year * 366 + dayOfYear) % allHadiths.size).let { if (it < 0) it + allHadiths.size else it }
        return allHadiths[index]
    }

    fun getHadithsByCategory(category: String, favoriteIds: Set<Int> = emptySet()): List<HadithItem> {
        return when (category) {
            CATEGORY_ALL -> allHadiths
            CATEGORY_FAVORIT -> allHadiths.filter { favoriteIds.contains(it.id) }
            else -> allHadiths.filter { it.category.equals(category, ignoreCase = true) }
        }
    }
}

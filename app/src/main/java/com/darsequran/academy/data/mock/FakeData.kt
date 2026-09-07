package com.darsequran.academy.data.mock

import com.darsequran.academy.data.model.AnnouncementAuthorDto
import com.darsequran.academy.data.model.AnnouncementDto
import com.darsequran.academy.data.model.AnnouncementImageDto
import com.darsequran.academy.data.model.BlogAuthorDto
import com.darsequran.academy.data.model.BlogPostDto
import com.darsequran.academy.data.model.FatwaItemDto

// TODO: Remove fake fallback data once server API endpoints return live data
object FakeData {

    // TODO: Remove fake announcements data when server API has live announcements
    val fakeAnnouncements: List<AnnouncementDto> = listOf(
        AnnouncementDto(
            id = "announcement-friday-bayaan",
            title = "Friday Bayaan",
            body = """
*Jummah Bayaan*

📍 Join us for an enlightening Jummah Bayaan at Markazi Jamia Masjid Shareef, Treran Tangmarg.

Guest Speaker:
Hazrat Mufti Abdur Rashid Qasmi Naqshbandi Sahib (DB)

Alhamdulillah, Darse Quran Academy *is honored to function under his esteemed supervision.

📖 Admissions Open – Hifz-e-Quran Batch 2
Register now: www.darsequranacademy.com
📱 WhatsApp: 9622966911
            """.trimIndent(),
            location = "MARKAZI JAMIA MASJID SHAREEF, TRERAN TANGMARG 193402",
            tag = "31 JULY 2026",
            priority = "High",
            createdAt = "28 July 2026",
            images = listOf(
                AnnouncementImageDto(
                    id = "img-bayaan",
                    imagePath = "https://images.unsplash.com/photo-1584551246679-0daf3d275d0f",
                    caption = "Jummah Bayaan Poster"
                )
            ),
            createdBy = AnnouncementAuthorDto(name = "Darse Quran Academy")
        ),
        AnnouncementDto(
            id = "announcement-1",
            title = "Ramadan Quran Recitation & Tajweed Competition 2026",
            body = "We are delighted to announce our annual Ramadan Quran Recitation Competition. Open for all registered students across beginner and advanced levels. Exciting rewards and scholar certificates will be awarded by senior Moulanas.",
            location = "Main Auditorium & Online Live Stream",
            tag = "Event",
            priority = "High",
            createdAt = "2026-09-01",
            images = listOf(
                AnnouncementImageDto(id = "img-1", imagePath = "https://images.unsplash.com/photo-1584551246679-0daf3d275d0f", caption = "Ramadan Event")
            ),
            createdBy = AnnouncementAuthorDto(name = "Academy Administration")
        ),
        AnnouncementDto(
            id = "announcement-2",
            title = "New Course Release: Advanced Arabic Morphology (Sarf)",
            body = "Enrollment is now open for our new 8-week intensive Sarf course taught by Moulana Abdul Rahman. Learn word structures, verb root patterns, and Quranic grammar principles.",
            location = "Online Campus",
            tag = "Course Release",
            priority = "Important",
            createdAt = "2026-08-28",
            createdBy = AnnouncementAuthorDto(name = "Academic Department")
        ),
        AnnouncementDto(
            id = "announcement-3",
            title = "Special Live Webinar: Fiqh of Zakat & Modern Investments",
            body = "Join Mufti Mohammad Salman for an interactive Q&A session on calculating Zakat on modern assets, stock investments, digital assets, and retirement accounts.",
            location = "Zoom Live Session",
            tag = "Webinar",
            priority = "Normal",
            createdAt = "2026-08-20",
            createdBy = AnnouncementAuthorDto(name = "Fatwa Board")
        ),
        AnnouncementDto(
            id = "announcement-4",
            title = "Sisters' Intensive Hifz & Tajweed Morning Batch",
            body = "New morning timings available for sisters interested in Hifz memorization and Tajweed correction. Classes led by certified female Qariah instructors.",
            location = "Sisters Wing & Virtual Classroom",
            tag = "Program",
            priority = "Normal",
            createdAt = "2026-08-15",
            createdBy = AnnouncementAuthorDto(name = "Sisters Department")
        )
    )

    // TODO: Remove fake blog posts data when server API has live blog posts
    val fakeBlogPosts: List<BlogPostDto> = listOf(
        BlogPostDto(
            id = "blog-mawlid-al-nabi",
            title = "Rabi' al-Awwal and the Celebration of Mawlid al-Nabi ﷺ",
            excerpt = "This article examines the significance of the blessed month of Rabi' al-Awwal and emphasizes that true love for Prophet Muhammad ﷺ is demonstrated through sincere obedience, following the Sunnah, and embodying prophetic character in daily life.",
            body = """
                The month of Rabi' al-Awwal holds a special place in the hearts of Muslims worldwide as the birth month of the final Messenger of Allah, Prophet Muhammad (PBUH).

                True celebration of the Prophet's legacy lies in reviving his Sunnah, increasing sending blessings (Salawat) upon him, practicing kindness, and seeking sacred Islamic knowledge.
            """.trimIndent(),
            category = "Reflections",
            readTime = "6 min read",
            createdAt = "10 July 2026",
            images = listOf(
                com.darsequran.academy.data.model.BlogImageDto(
                    id = "img-mawlid",
                    imagePath = "https://images.unsplash.com/photo-1584551246679-0daf3d275d0f",
                    caption = "Rabi' al-Awwal Banner"
                )
            ),
            createdBy = BlogAuthorDto(name = "Talib Ul Islam", email = "talib@darsequran.com")
        ),
        BlogPostDto(
            id = "blog-1",
            title = "5 Essential Tajweed Rules Every Reciter Should Master",
            excerpt = "Mastering proper pronunciation and Makharij is the foundation of beautiful Quranic recitation. Here are five practical tips from our senior Tajweed scholars.",
            body = """
                Reciting the Holy Quran with correct Tajweed is not merely an art, but a religious discipline that preserves the divine revelation as taught by Prophet Muhammad (PBUH).

                1. Makhraj al-Huroof (Points of Articulation): Pay careful attention to throat letters (Halqiyyah) such as Ayn and Ha.
                2. Ghunnah (Nasalization): Give full two-count nasal sound to Noon and Meem Mushaddad.
                3. Qalqalah (Echoing Sound): Apply proper Qalqalah on letters Qaf, Tt, Ba, Jeem, and Dal when Sakin.
                4. Madd Rules (Elongation): Differentiate clearly between natural elongation (Madd Asli) and secondary elongation (Madd Far'i).
                5. Ikhfa and Idgham: Practice subtle concealment (Ikhfa) before the 15 Ikhfa letters.

                Consistent daily practice under a qualified teacher is essential to perfect these rules.
            """.trimIndent(),
            category = "Tajweed Tips",
            readTime = "5 min read",
            createdAt = "2026-09-02",
            createdBy = BlogAuthorDto(name = "Mufti Mohammad Salman", email = "salman@darsequran.com")
        ),
        BlogPostDto(
            id = "blog-2",
            title = "The Spiritual Power of Morning and Evening Adhkar",
            excerpt = "In the hustle of modern life, daily prophetic supplications serve as an impenetrable spiritual shield and source of heart tranquility.",
            body = """
                Allah says in the Quran: 'Unquestionably, by the remembrance of Allah do hearts find rest.' (Surah Ar-Ra'd 13:28).

                The morning and evening Adhkar prescribed by the Prophet (PBUH) provide physical protection, spiritual focus, and relief from anxiety. Making a habit of reciting Ayat al-Kursi, the last three Surahs of the Quran, and morning supplications creates a blessed rhythm in a believer's day.
            """.trimIndent(),
            category = "Spiritual Growth",
            readTime = "4 min read",
            createdAt = "2026-08-30",
            createdBy = BlogAuthorDto(name = "Moulana Abdul Rahman", email = "rahman@darsequran.com")
        ),
        BlogPostDto(
            id = "blog-3",
            title = "Building a Consistent Daily Quran Revision Routine",
            excerpt = "Memorizing Quranic verses is a noble goal, but retaining them requires structured revision strategies and disciplined daily habits.",
            body = """
                The Prophet (PBUH) warned that the Quran slips away faster than untethered camels if not revised regularly. Here is a proven 3-tier revision method used by classical Huffaz:

                1. Sabq (New Lesson): Memorize fresh verses with 100% precision in the morning.
                2. Sabqi (Recent Lessons): Revise the past 5-10 pages memorized over the last week.
                3. Manzil (Older Revision): Read 1 Juz daily from earlier memorization.

                Maintaining this routine ensures strong retention and spiritual connection with the Quran.
            """.trimIndent(),
            category = "Reflections",
            readTime = "6 min read",
            createdAt = "2026-08-25",
            createdBy = BlogAuthorDto(name = "Sheikh Farooq Ahmad", email = "farooq@darsequran.com")
        ),
        BlogPostDto(
            id = "blog-4",
            title = "Etiquettes of Seeking Islamic Knowledge (Adab al-Talib)",
            excerpt = "Sincerity (Ikhlas), respect for scholars, and humility are the true keys to unlocking beneficial Islamic knowledge.",
            body = """
                Knowledge without Adab (etiquette and manners) fails to benefit the heart. Classical scholars emphasized that learning manners precedes learning sacred knowledge.

                Purify your intention solely for the pleasure of Allah, respect your teachers, practice what you learn, and stay humble when seeking religious understanding.
            """.trimIndent(),
            category = "Adab & Ethics",
            readTime = "7 min read",
            createdAt = "2026-08-18",
            createdBy = BlogAuthorDto(name = "Dr. Zakir Hussain", email = "zakir@darsequran.com")
        )
    )

    // TODO: Remove fake fatwas data when server API has live fatwa queries
    val fakeFatwas: List<FatwaItemDto> = listOf(
        FatwaItemDto(
            id = "fatwa-salam-handshake",
            title = "How to shake hands during salam?",
            question = "Please tell us how to shake hands during salam. In the US we are from different countries and evidently Asians shake hands with two and other people shake hands by one. I am confused which way is the correct according to sunnah and more adequate.",
            answer = "Answer ID: 145587--(Fatwa: 166/156/L=2/1438) Bismillah hir-Rahman nir-Rahim !\n\nMusafaha(shaking hand) is sunnah with two hands. It is narrated in Bukhari Sharif: علمنى النبي صلى الله عليه وسلم التشهد وكفى بين كفيه. The hadith clearly speaks that at the time of shaking hand the palm of Hadhrat Abdullah Ibn Masood (may Allah be pleased with him) was between both the palms of the Holy Prophet Muhammad (may peace and blessings of Allah be upon him). And it seems impossible that the Holy Prophet Muhammad (may peace and blessings of Allah be upon him) did musafaha with both the hands and Hadhrat Abdullah Ibn Masood (may Allah be pleased with him) did it with single hand. In case we acknowledge it then also the act of the Prophet (may peace and blessings of Allah be upon him) shall be preferred. Imam Bukhari has proved doing musafaha by this Hadith and he has also quoted that “صافح حماد بكلتا يديه” which says that it was the way of the salaf. Hence you should do musafaha with both the hands and should not be confused.\n\nAllah (Subhana Wa Ta'ala) knows Best\nDarul Ifta, Darul Uloom Deoband, India",
            category = "RIGHTS & ETIQUETTES",
            scholarName = "DARUL IFTA-DQA",
            answeredAt = "28 June 2026",
            approvalStatus = "APPROVED"
        ),
        FatwaItemDto(
            id = "fatwa-1",
            title = "Ruling on combining missed prayers during travel",
            question = "As-salamu alaykum. I travel frequently for work across cities. What are the conditions under which I can shorten (Qasr) or combine (Jam') my prayers according to Fiqh?",
            answer = "Wa alaykum as-salam. Shortening 4-rak'ah prayers to 2 rak'ahs (Qasr) is a divine concession during valid travel exceeding approximately 77–89 km outside city limits. Combining prayers (Jam' bayn as-Salatayn) is permissible during active travel or severe hardship under Shafi'i, Maliki, and Hanbali Fiqh, whereas Hanafi Fiqh requires praying each prayer in its designated time except during Hajj at Arafat and Muzdalifah.",
            category = "Fiqh & Worship",
            askerName = "Brother Ahmad",
            scholarName = "Moulana Abdul Rahman",
            answeredAt = "2026-09-03",
            approvalStatus = "APPROVED"
        ),
        FatwaItemDto(
            id = "fatwa-2",
            title = "Is Tajweed obligatory when reciting Quran in daily Salah?",
            question = "Is it sinful if a person makes minor Tajweed mistakes while reciting Surah Al-Fatihah during individual or congregational Salah?",
            answer = "Reciting Surah Al-Fatihah correctly such that the meanings are preserved is an essential pillar (Rukn) of Salah. Mistakes that alter the clear meaning (Lahn Jali) must be corrected. Minor phonetic flaws (Lahn Khafi) like misjudging Madd length do not invalidate Salah, but learning basic Tajweed remains a communal obligation (Fard Kifayah) and highly recommended for every Muslim.",
            category = "Tajweed & Salah",
            askerName = "Sister Maryam",
            scholarName = "Sheikh Farooq Ahmad",
            answeredAt = "2026-08-29",
            approvalStatus = "APPROVED"
        ),
        FatwaItemDto(
            id = "fatwa-3",
            title = "Calculating Zakat on modern digital investments and savings",
            question = "How should Zakat be calculated on stocks, mutual funds, and digital bank savings accounts held for more than one lunar year?",
            answer = "Zakat (2.5%) is payable on liquid savings and investment accounts if total wealth exceeds Nisab (equivalent to 85 grams of gold or 595 grams of silver) for one full lunar year. For active trading stocks, Zakat is calculated on current market value. For long-term dividend investments, Zakat is calculated on liquid cash dividends and current asset portions.",
            category = "Zakat & Finance",
            askerName = "Zaid Khan",
            scholarName = "Mufti Mohammad Salman",
            answeredAt = "2026-08-22",
            approvalStatus = "APPROVED"
        ),
        FatwaItemDto(
            id = "fatwa-4",
            title = "Etiquettes of seeking Islamic knowledge online",
            question = "What guidelines should students follow when listening to lectures or attending online Islamic courses via mobile apps and video streams?",
            answer = "Students seeking sacred knowledge online should maintain reverence for religious text, listen attentively without distractions, verify information from qualified scholars, and avoid argumentative debates in comment sections. Intention should be pure learning for spiritual elevation.",
            category = "General",
            askerName = "Anonymous Student",
            scholarName = "Dr. Zakir Hussain",
            answeredAt = "2026-08-14",
            approvalStatus = "APPROVED"
        )
    )
}

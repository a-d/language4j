/**
 * Internationalization (i18n) Service
 * ====================================
 * Simple translation system for the Language Learning Platform.
 * Uses the user's native language for UI text.
 */

/**
 * Translation dictionaries for supported languages
 */
const translations = {
    en: {
        // Navigation
        'nav.dashboard': 'Dashboard',
        'nav.lessons': 'Lessons',
        'nav.vocabulary': 'Vocabulary',
        'nav.cards': 'Cards',
        'nav.exercises': 'Exercises',
        'nav.progress': 'Progress',
        'nav.settings': 'Settings',
        
        // Dashboard
        'dashboard.welcome': 'Welcome Back!',
        'dashboard.learning': 'Learning {target} from {native}',
        'dashboard.todaysGoals': "Today's Goals",
        'dashboard.continuelearning': 'Continue Learning',
        'dashboard.recentActivity': 'Recent Activity',
        'dashboard.noGoals': 'No daily goals set.',
        'dashboard.createDefaultGoals': 'Create Default Goals',
        'dashboard.activityComingSoon': 'Activity tracking coming soon!',
        'dashboard.activityHint': 'Complete lessons and exercises to see your activity here.',
        
        // Quick Actions
        'action.startLesson': 'Start Lesson',
        'action.startLessonDesc': 'Continue where you left off',
        'action.flashcards': 'Flashcards',
        'action.flashcardsDesc': 'Review vocabulary',
        'action.speaking': 'Speaking',
        'action.speakingDesc': 'Pronunciation practice',
        'action.roleplay': 'Roleplay',
        'action.roleplayDesc': 'Conversation scenarios',
        
        // Lessons
        'lessons.title': 'Lessons',
        'lessons.generateNew': 'Generate New Lesson',
        'lessons.generateTitle': 'Generate a New Lesson',
        'lessons.topic': 'Topic',
        'lessons.topicPlaceholder': 'e.g., greetings, food, travel',
        'lessons.generate': '🎯 Generate Lesson',
        'lessons.listen': '🔊 Listen',
        
        // Vocabulary
        'vocabulary.title': 'Vocabulary',
        'vocabulary.addWords': 'Add Words',
        'vocabulary.reviewFlashcards': 'Review Flashcards',
        'vocabulary.generateTitle': 'Generate Vocabulary',
        'vocabulary.words': 'Words',
        'vocabulary.generate': '📝 Generate Vocabulary',
        'vocabulary.flashcardsTitle': 'Flashcards',
        'vocabulary.generateFlashcards': '🃏 Generate Flashcards',
        
        // Exercises
        'exercises.title': 'Exercises',
        'exercises.fillBlanks': 'Fill in the Blanks',
        'exercises.fillBlanksDesc': 'Complete sentences with the right words',
        'exercises.wordOrder': 'Word Order',
        'exercises.wordOrderDesc': 'Arrange words to form sentences',
        'exercises.translation': 'Translation',
        'exercises.translationDesc': 'Translate sentences',
        'exercises.listening': 'Listening',
        'exercises.listeningDesc': 'Listen and transcribe',
        'exercises.speakingExercise': 'Speaking',
        'exercises.speakingDesc': 'Pronounce words and sentences',
        'exercises.exit': '✕ Exit',
        'exercises.done': 'Done',
        'exercises.generating': 'Generating exercise...',
        'exercises.score': 'Score',
        'exercises.submitAll': 'Submit All',
        'exercises.correctAnswers': 'Correct Answers',
        'exercises.timeSpent': 'Time Spent',
        'exercises.greatJob': 'Great Job! 🎉',
        'exercises.keepPracticing': 'Keep Practicing!',
        'exercises.tryAgain': 'Try Again',
        'exercises.recentHistory': 'Recent History',
        'exercises.noHistory': 'No exercise history yet. Complete some exercises to see your progress!',
        'exercises.historyLoadFailed': 'Failed to load exercise history',
        'exercises.check': 'Check',
        'exercises.noExercisesGenerated': 'No exercises generated.',
        'exercises.clickToListen': 'Click to listen',
        'exercises.playingAudio': 'Playing...',
        'exercises.clickToReplay': 'Click to replay',
        'exercises.paused': 'Paused',
        'exercises.typeWhatYouHear': 'Type what you hear...',
        'exercises.listenExample': 'Listen to example',
        'exercises.recordYourVoice': 'Record your voice',
        'exercises.yourSpeech': 'Your speech',
        'exercises.noSpeechDetected': 'No speech detected',
        'exercises.transcriptionFailed': 'Transcription failed',
        'exercises.recordFirst': 'Please record your voice first',
        'exercises.evaluating': 'Evaluating',
        
        // Progress
        'progress.title': 'Your Progress',
        'progress.exercisesCompleted': 'Exercises Completed',
        'progress.averageScore': 'Average Score',
        'progress.timeSpent': 'Time Spent',
        'progress.dayStreak': 'Day Streak',
        'progress.goals': 'Goals',
        'progress.noLongTermGoals': 'No long-term goals set yet.',
        'progress.createGoal': 'Create Goal',
        
        // Settings
        'settings.title': 'Settings',
        'settings.subtitle': 'Manage your profile and preferences',
        'settings.profile': '👤 Profile',
        'settings.editProfile': '✏️ Edit Profile',
        'settings.memberSince': 'Member since:',
        'settings.level': 'Level:',
        'settings.preferences': '⚙️ Preferences',
        'settings.darkMode': 'Dark Mode',
        'settings.darkModeDesc': 'Switch between light and dark themes',
        'settings.notifications': 'Notifications',
        'settings.notificationsDesc': 'Daily reminders and goal notifications',
        'settings.speechVoice': 'Speech Voice',
        'settings.speechVoiceDesc': 'Choose your preferred TTS voice',
        'settings.comingSoon': 'Coming Soon',
        'settings.about': 'ℹ️ About',
        'settings.appName': 'Language Learning Platform',
        'settings.appDesc': 'An AI-powered language learning experience',
        'settings.version': 'Version 1.0.0',
        
        // Edit Profile Modal
        'profile.editTitle': '👤 Edit Profile',
        'profile.displayName': 'Display Name',
        'profile.displayNamePlaceholder': 'Enter your name',
        'profile.skillLevel': 'Skill Level (CEFR)',
        'profile.languages': 'Languages',
        'profile.languagesNote': '(Languages are configured at deployment time)',
        'profile.save': 'Save Changes',
        'profile.cancel': 'Cancel',
        
        // Skill Levels
        'level.A1': 'Beginner',
        'level.A2': 'Elementary',
        'level.B1': 'Intermediate',
        'level.B2': 'Upper Intermediate',
        'level.C1': 'Advanced',
        'level.C2': 'Proficient',
        
        // Goals
        'goals.createTitle': 'Create New Goal',
        'goals.editTitle': 'Edit Goal',
        'goals.title': 'Title',
        'goals.titlePlaceholder': 'e.g., Learn 100 words',
        'goals.description': 'Description',
        'goals.descriptionPlaceholder': 'Optional description for this goal',
        'goals.type': 'Type',
        'goals.typeDaily': 'Daily',
        'goals.typeWeekly': 'Weekly',
        'goals.typeMonthly': 'Monthly',
        'goals.typeYearly': 'Yearly',
        'goals.typeCannotChange': 'Goal type cannot be changed after creation',
        'goals.target': 'Target',
        'goals.unit': 'Unit',
        'goals.unitPlaceholder': 'e.g., words, lessons',
        'goals.currentProgress': 'Current Progress',
        'goals.newProgress': 'New Progress',
        'goals.create': 'Create',
        'goals.save': 'Save',
        'goals.edit': 'Edit',
        'goals.delete': 'Delete',
        'goals.reset': 'Reset',
        'goals.complete': 'Complete',
        'goals.done': 'Done',
        'goals.completed': 'completed',
        'goals.addGoal': 'Add Goal',
        'goals.createCustom': 'Create Custom Goal',
        'goals.allGoals': 'All',
        'goals.increment': 'Add 1',
        'goals.updateProgress': 'Update Progress',
        'goals.markComplete': 'Mark as Complete',
        'goals.moreActions': 'More Actions',
        'goals.clickToUpdate': 'Click to update progress',
        'goals.confirmReset': 'Are you sure you want to reset this goal\'s progress to 0?',
        
        // Toast Messages
        'toast.profileUpdated': 'Profile updated successfully!',
        'toast.profileUpdateFailed': 'Failed to update profile. Please try again.',
        'toast.goalCreated': 'Goal created!',
        'toast.goalCreateFailed': 'Failed to create goal',
        'toast.goalCompleted': 'Goal completed! 🎉',
        'toast.goalDeleted': 'Goal deleted',
        'toast.goalUpdated': 'Goal updated!',
        'toast.goalUpdateFailed': 'Failed to update goal',
        'toast.goalReset': 'Goal progress reset',
        'toast.goalNotFound': 'Goal not found',
        'toast.progressUpdated': 'Progress updated!',
        'toast.lessonGenerated': 'Lesson generated successfully!',
        'toast.lessonGenerateFailed': 'Failed to generate lesson. Please try again.',
        'toast.vocabGenerated': 'Vocabulary generated!',
        'toast.vocabGenerateFailed': 'Failed to generate vocabulary. Please try again.',
        'toast.flashcardsGenerated': 'Flashcards generated!',
        'toast.flashcardsGenerateFailed': 'Failed to generate flashcards. Please try again.',
        'toast.exerciseLoaded': 'Exercise loaded!',
        'toast.exerciseGenerateFailed': 'Failed to generate exercise. Please try again.',
        'toast.exerciseSaved': 'Exercise result saved!',
        'toast.exerciseSaveFailed': 'Failed to save exercise result',
        'toast.scenarioGenerated': 'Scenario generated!',
        'toast.scenarioGenerateFailed': 'Failed to generate scenario',
        'toast.defaultGoalsCreated': 'Default goals created!',
        'toast.backendUnavailable': 'Backend unavailable - showing offline mode',
        'toast.generatingAudio': 'Generating audio...',
        'toast.playingAudio': 'Playing audio',
        'toast.speechUnavailable': 'Speech synthesis unavailable',
        'toast.userDataNotLoaded': 'User data not loaded',
        'toast.enterDisplayName': 'Please enter a display name',
        'toast.enterTitle': 'Please enter a title',
        'toast.audioPlayFailed': 'Failed to play audio',
        'toast.audioGenerationFailed': 'Failed to generate audio',
        'toast.recordingStarted': 'Recording started - speak now',
        'toast.recordingStopped': 'Recording stopped',
        'toast.microphoneAccessDenied': 'Microphone access denied',
        'toast.transcribing': 'Transcribing your speech...',
        'toast.transcriptionComplete': 'Transcription complete',
        'toast.transcriptionFailed': 'Transcription failed',
        
        // Progress Page
        'progress.noGoals': 'No goals set yet.',
        'progress.noGoalsInCategory': 'No goals in this category.',
        
        // Activity
        'activity.completedGoal': 'Completed goal',
        'activity.today': 'Today',
        
        // Misc
        'misc.loading': 'Loading...',
        'misc.close': 'Close',
        'misc.generating': 'Generating...',
        'misc.comingSoon': 'coming soon!',
        'misc.roleplayTitle': '🎭 Roleplay:',
        'misc.clearCache': 'Clear',
        
        // Toast - Cache
        'toast.cacheCleared': 'Content cleared',
        
        // Visual Learning Cards
        'cards.title': 'Visual Learning Cards',
        'cards.generateTitle': 'Generate Visual Cards',
        'cards.generateFromTopic': 'Generate from Topic',
        'cards.topicLabel': 'Topic',
        'cards.topicPlaceholder': 'e.g., Kitchen items, Animals, Colors',
        'cards.topicDescription': 'Enter a topic and we\'ll create vocabulary cards with AI-generated images.',
        'cards.cardCount': 'Number of cards',
        'cards.customCardTitle': 'Custom Card (Advanced)',
        'cards.word': 'Word',
        'cards.wordPlaceholder': 'e.g., apple, house, car',
        'cards.context': 'Context (optional)',
        'cards.contextPlaceholder': 'e.g., The apple is red.',
        'cards.generate': '🖼️ Generate Image',
        'cards.generateBatch': '🖼️ Generate Multiple',
        'cards.wordsList': 'Words (comma-separated)',
        'cards.wordsListPlaceholder': 'e.g., apple, banana, orange',
        'cards.flipCard': 'Flip Card',
        'cards.flipHint': 'Click card to reveal translation',
        'cards.showImage': 'Show Image',
        'cards.hideImage': 'Hide Image',
        'cards.nextCard': 'Next',
        'cards.prevCard': 'Previous',
        'cards.cardOf': 'Card {current} of {total}',
        'cards.noCards': 'No visual cards yet. Enter a topic above to generate learning cards!',
        'cards.generating': 'Generating image...',
        'cards.generatingFromTopic': 'Generating {count} cards... This may take a minute.',
        'cards.generatingBatch': 'Generating images...',
        'cards.imageGenerated': 'Image generated!',
        'cards.imageGenerateFailed': 'Failed to generate image',
        'cards.topicGenerateFailed': 'Failed to generate cards. Please try again.',
        'cards.topicSuccess': '{count} visual cards generated!',
        'cards.partialSuccess': '{success} of {total} cards generated (some images failed)',
        'cards.imageServiceUnavailable': 'Image service unavailable. Please configure image generation.',
        'cards.batchComplete': 'All images generated!',
        'cards.visualFlashcards': 'Visual Flashcards',
        'cards.visualFlashcardsDesc': 'Learn with AI-generated images',
        'cards.yourLanguage': 'Your language',
        'cards.targetLanguage': 'Learning',
        
        // Languages
        'lang.de': 'German',
        'lang.fr': 'French',
        'lang.en': 'English',
        'lang.es': 'Spanish',
        'lang.it': 'Italian',
        'lang.pt': 'Portuguese',
        'lang.nl': 'Dutch',
        'lang.pl': 'Polish',
        'lang.ru': 'Russian',
        'lang.ja': 'Japanese',
        'lang.zh': 'Chinese',
        'lang.ko': 'Korean'
    },
    
    de: {
        // Navigation
        'nav.dashboard': 'Übersicht',
        'nav.lessons': 'Lektionen',
        'nav.vocabulary': 'Vokabeln',
        'nav.cards': 'Karten',
        'nav.exercises': 'Übungen',
        'nav.progress': 'Fortschritt',
        'nav.settings': 'Einstellungen',
        
        // Dashboard
        'dashboard.welcome': 'Willkommen zurück!',
        'dashboard.learning': '{target} lernen von {native}',
        'dashboard.todaysGoals': 'Heutige Ziele',
        'dashboard.continuelearning': 'Weiterlernen',
        'dashboard.recentActivity': 'Letzte Aktivitäten',
        'dashboard.noGoals': 'Keine täglichen Ziele gesetzt.',
        'dashboard.createDefaultGoals': 'Standardziele erstellen',
        'dashboard.activityComingSoon': 'Aktivitätsverfolgung kommt bald!',
        'dashboard.activityHint': 'Schließe Lektionen und Übungen ab, um deine Aktivitäten hier zu sehen.',
        
        // Quick Actions
        'action.startLesson': 'Lektion starten',
        'action.startLessonDesc': 'Dort weitermachen, wo du aufgehört hast',
        'action.flashcards': 'Karteikarten',
        'action.flashcardsDesc': 'Vokabeln wiederholen',
        'action.speaking': 'Sprechen',
        'action.speakingDesc': 'Aussprache üben',
        'action.roleplay': 'Rollenspiel',
        'action.roleplayDesc': 'Gesprächsszenarien',
        
        // Lessons
        'lessons.title': 'Lektionen',
        'lessons.generateNew': 'Neue Lektion generieren',
        'lessons.generateTitle': 'Neue Lektion generieren',
        'lessons.topic': 'Thema',
        'lessons.topicPlaceholder': 'z.B. Begrüßungen, Essen, Reisen',
        'lessons.generate': '🎯 Lektion generieren',
        'lessons.listen': '🔊 Anhören',
        
        // Vocabulary
        'vocabulary.title': 'Vokabeln',
        'vocabulary.addWords': 'Wörter hinzufügen',
        'vocabulary.reviewFlashcards': 'Karteikarten üben',
        'vocabulary.generateTitle': 'Vokabeln generieren',
        'vocabulary.words': 'Wörter',
        'vocabulary.generate': '📝 Vokabeln generieren',
        'vocabulary.flashcardsTitle': 'Karteikarten',
        'vocabulary.generateFlashcards': '🃏 Karteikarten generieren',
        
        // Exercises
        'exercises.title': 'Übungen',
        'exercises.fillBlanks': 'Lückentext',
        'exercises.fillBlanksDesc': 'Sätze mit den richtigen Wörtern vervollständigen',
        'exercises.wordOrder': 'Wortstellung',
        'exercises.wordOrderDesc': 'Wörter zu Sätzen anordnen',
        'exercises.translation': 'Übersetzung',
        'exercises.translationDesc': 'Sätze übersetzen',
        'exercises.listening': 'Hörverständnis',
        'exercises.listeningDesc': 'Zuhören und aufschreiben',
        'exercises.speakingExercise': 'Sprechen',
        'exercises.speakingDesc': 'Wörter und Sätze aussprechen',
        'exercises.exit': '✕ Beenden',
        'exercises.done': 'Fertig',
        'exercises.generating': 'Übung wird generiert...',
        'exercises.score': 'Punktzahl',
        'exercises.submitAll': 'Alle abgeben',
        'exercises.correctAnswers': 'Richtige Antworten',
        'exercises.timeSpent': 'Zeit verbracht',
        'exercises.greatJob': 'Super gemacht! 🎉',
        'exercises.keepPracticing': 'Weiter üben!',
        'exercises.tryAgain': 'Nochmal versuchen',
        'exercises.recentHistory': 'Letzte Übungen',
        'exercises.noHistory': 'Noch keine Übungshistorie. Schließe einige Übungen ab, um deinen Fortschritt zu sehen!',
        'exercises.historyLoadFailed': 'Fehler beim Laden der Übungshistorie',
        'exercises.check': 'Prüfen',
        'exercises.noExercisesGenerated': 'Keine Übungen generiert.',
        'exercises.clickToListen': 'Klicken zum Anhören',
        'exercises.playingAudio': 'Wird abgespielt...',
        'exercises.clickToReplay': 'Nochmal anhören',
        'exercises.paused': 'Pausiert',
        'exercises.typeWhatYouHear': 'Schreibe, was du hörst...',
        'exercises.listenExample': 'Beispiel anhören',
        'exercises.recordYourVoice': 'Stimme aufnehmen',
        'exercises.yourSpeech': 'Deine Aussprache',
        'exercises.noSpeechDetected': 'Keine Sprache erkannt',
        'exercises.transcriptionFailed': 'Transkription fehlgeschlagen',
        'exercises.recordFirst': 'Bitte nimm zuerst deine Stimme auf',
        'exercises.evaluating': 'Wird ausgewertet',
        
        // Progress
        'progress.title': 'Dein Fortschritt',
        'progress.exercisesCompleted': 'Abgeschlossene Übungen',
        'progress.averageScore': 'Durchschnitt',
        'progress.timeSpent': 'Zeit verbracht',
        'progress.dayStreak': 'Tage in Folge',
        'progress.goals': 'Ziele',
        'progress.noLongTermGoals': 'Noch keine langfristigen Ziele gesetzt.',
        'progress.createGoal': 'Ziel erstellen',
        
        // Settings
        'settings.title': 'Einstellungen',
        'settings.subtitle': 'Profil und Einstellungen verwalten',
        'settings.profile': '👤 Profil',
        'settings.editProfile': '✏️ Profil bearbeiten',
        'settings.memberSince': 'Mitglied seit:',
        'settings.level': 'Stufe:',
        'settings.preferences': '⚙️ Einstellungen',
        'settings.darkMode': 'Dunkelmodus',
        'settings.darkModeDesc': 'Zwischen hellem und dunklem Design wechseln',
        'settings.notifications': 'Benachrichtigungen',
        'settings.notificationsDesc': 'Tägliche Erinnerungen und Ziel-Benachrichtigungen',
        'settings.speechVoice': 'Sprachstimme',
        'settings.speechVoiceDesc': 'Bevorzugte TTS-Stimme auswählen',
        'settings.comingSoon': 'Kommt bald',
        'settings.about': 'ℹ️ Über',
        'settings.appName': 'Sprachlern-Plattform',
        'settings.appDesc': 'KI-gestütztes Sprachenlernen',
        'settings.version': 'Version 1.0.0',
        
        // Edit Profile Modal
        'profile.editTitle': '👤 Profil bearbeiten',
        'profile.displayName': 'Anzeigename',
        'profile.displayNamePlaceholder': 'Gib deinen Namen ein',
        'profile.skillLevel': 'Sprachniveau (GER)',
        'profile.languages': 'Sprachen',
        'profile.languagesNote': '(Sprachen werden bei der Einrichtung konfiguriert)',
        'profile.save': 'Änderungen speichern',
        'profile.cancel': 'Abbrechen',
        
        // Skill Levels
        'level.A1': 'Anfänger',
        'level.A2': 'Grundkenntnisse',
        'level.B1': 'Fortgeschritten',
        'level.B2': 'Selbständig',
        'level.C1': 'Fachkundig',
        'level.C2': 'Muttersprachlich',
        
        // Goals
        'goals.createTitle': 'Neues Ziel erstellen',
        'goals.editTitle': 'Ziel bearbeiten',
        'goals.title': 'Titel',
        'goals.titlePlaceholder': 'z.B. 100 Wörter lernen',
        'goals.description': 'Beschreibung',
        'goals.descriptionPlaceholder': 'Optionale Beschreibung für dieses Ziel',
        'goals.type': 'Typ',
        'goals.typeDaily': 'Täglich',
        'goals.typeWeekly': 'Wöchentlich',
        'goals.typeMonthly': 'Monatlich',
        'goals.typeYearly': 'Jährlich',
        'goals.typeCannotChange': 'Zieltyp kann nach Erstellung nicht geändert werden',
        'goals.target': 'Zielwert',
        'goals.unit': 'Einheit',
        'goals.unitPlaceholder': 'z.B. Wörter, Lektionen',
        'goals.currentProgress': 'Aktueller Fortschritt',
        'goals.newProgress': 'Neuer Fortschritt',
        'goals.create': 'Erstellen',
        'goals.save': 'Speichern',
        'goals.edit': 'Bearbeiten',
        'goals.delete': 'Löschen',
        'goals.reset': 'Zurücksetzen',
        'goals.complete': 'Abschließen',
        'goals.done': 'Erledigt',
        'goals.completed': 'abgeschlossen',
        'goals.addGoal': 'Ziel hinzufügen',
        'goals.createCustom': 'Eigenes Ziel erstellen',
        'goals.allGoals': 'Alle',
        'goals.increment': '+1 hinzufügen',
        'goals.updateProgress': 'Fortschritt aktualisieren',
        'goals.markComplete': 'Als erledigt markieren',
        'goals.moreActions': 'Weitere Aktionen',
        'goals.clickToUpdate': 'Klicken zum Aktualisieren',
        'goals.confirmReset': 'Möchtest du den Fortschritt dieses Ziels wirklich auf 0 zurücksetzen?',
        
        // Toast Messages
        'toast.profileUpdated': 'Profil erfolgreich aktualisiert!',
        'toast.profileUpdateFailed': 'Profil konnte nicht aktualisiert werden. Bitte versuche es erneut.',
        'toast.goalCreated': 'Ziel erstellt!',
        'toast.goalCreateFailed': 'Ziel konnte nicht erstellt werden',
        'toast.goalCompleted': 'Ziel erreicht! 🎉',
        'toast.goalDeleted': 'Ziel gelöscht',
        'toast.goalUpdated': 'Ziel aktualisiert!',
        'toast.goalUpdateFailed': 'Ziel konnte nicht aktualisiert werden',
        'toast.goalReset': 'Zielfortschritt zurückgesetzt',
        'toast.goalNotFound': 'Ziel nicht gefunden',
        'toast.progressUpdated': 'Fortschritt aktualisiert!',
        'toast.lessonGenerated': 'Lektion erfolgreich generiert!',
        'toast.lessonGenerateFailed': 'Lektion konnte nicht generiert werden. Bitte versuche es erneut.',
        'toast.vocabGenerated': 'Vokabeln generiert!',
        'toast.vocabGenerateFailed': 'Vokabeln konnten nicht generiert werden. Bitte versuche es erneut.',
        'toast.flashcardsGenerated': 'Karteikarten generiert!',
        'toast.flashcardsGenerateFailed': 'Karteikarten konnten nicht generiert werden. Bitte versuche es erneut.',
        'toast.exerciseLoaded': 'Übung geladen!',
        'toast.exerciseGenerateFailed': 'Übung konnte nicht generiert werden. Bitte versuche es erneut.',
        'toast.exerciseSaved': 'Übungsergebnis gespeichert!',
        'toast.exerciseSaveFailed': 'Fehler beim Speichern des Übungsergebnisses',
        'toast.scenarioGenerated': 'Szenario generiert!',
        'toast.scenarioGenerateFailed': 'Szenario konnte nicht generiert werden',
        'toast.defaultGoalsCreated': 'Standardziele erstellt!',
        'toast.backendUnavailable': 'Backend nicht verfügbar - Offline-Modus',
        'toast.generatingAudio': 'Audio wird generiert...',
        'toast.playingAudio': 'Audio wird abgespielt',
        'toast.speechUnavailable': 'Sprachsynthese nicht verfügbar',
        'toast.userDataNotLoaded': 'Benutzerdaten nicht geladen',
        'toast.enterDisplayName': 'Bitte gib einen Anzeigenamen ein',
        'toast.enterTitle': 'Bitte gib einen Titel ein',
        'toast.audioPlayFailed': 'Audio konnte nicht abgespielt werden',
        'toast.audioGenerationFailed': 'Audio konnte nicht generiert werden',
        'toast.recordingStarted': 'Aufnahme gestartet - jetzt sprechen',
        'toast.recordingStopped': 'Aufnahme beendet',
        'toast.microphoneAccessDenied': 'Mikrofonzugriff verweigert',
        'toast.transcribing': 'Sprache wird transkribiert...',
        'toast.transcriptionComplete': 'Transkription abgeschlossen',
        'toast.transcriptionFailed': 'Transkription fehlgeschlagen',
        
        // Progress Page
        'progress.noGoals': 'Noch keine Ziele gesetzt.',
        'progress.noGoalsInCategory': 'Keine Ziele in dieser Kategorie.',
        
        // Activity
        'activity.completedGoal': 'Ziel abgeschlossen',
        'activity.today': 'Heute',
        
        // Misc
        'misc.loading': 'Lädt...',
        'misc.close': 'Schließen',
        'misc.generating': 'Wird generiert...',
        'misc.comingSoon': 'kommt bald!',
        'misc.roleplayTitle': '🎭 Rollenspiel:',
        'misc.clearCache': 'Löschen',
        
        // Toast - Cache
        'toast.cacheCleared': 'Inhalt gelöscht',
        
        // Visual Learning Cards
        'cards.title': 'Visuelle Lernkarten',
        'cards.generateTitle': 'Visuelle Karten generieren',
        'cards.generateFromTopic': 'Aus Thema generieren',
        'cards.topicLabel': 'Thema',
        'cards.topicPlaceholder': 'z.B. Küchenutensilien, Tiere, Farben',
        'cards.topicDescription': 'Gib ein Thema ein und wir erstellen Vokabelkarten mit KI-generierten Bildern.',
        'cards.cardCount': 'Anzahl der Karten',
        'cards.customCardTitle': 'Einzelne Karte (Erweitert)',
        'cards.word': 'Wort',
        'cards.wordPlaceholder': 'z.B. Apfel, Haus, Auto',
        'cards.context': 'Kontext (optional)',
        'cards.contextPlaceholder': 'z.B. Der Apfel ist rot.',
        'cards.generate': '🖼️ Bild generieren',
        'cards.generateBatch': '🖼️ Mehrere generieren',
        'cards.wordsList': 'Wörter (kommagetrennt)',
        'cards.wordsListPlaceholder': 'z.B. Apfel, Banane, Orange',
        'cards.flipCard': 'Karte umdrehen',
        'cards.flipHint': 'Karte anklicken für Übersetzung',
        'cards.showImage': 'Bild zeigen',
        'cards.hideImage': 'Bild verbergen',
        'cards.nextCard': 'Weiter',
        'cards.prevCard': 'Zurück',
        'cards.cardOf': 'Karte {current} von {total}',
        'cards.noCards': 'Noch keine visuellen Karten. Gib oben ein Thema ein, um Lernkarten zu generieren!',
        'cards.generating': 'Bild wird generiert...',
        'cards.generatingFromTopic': '{count} Karten werden generiert... Das kann eine Minute dauern.',
        'cards.generatingBatch': 'Bilder werden generiert...',
        'cards.imageGenerated': 'Bild generiert!',
        'cards.imageGenerateFailed': 'Bilderzeugung fehlgeschlagen',
        'cards.topicGenerateFailed': 'Kartengenerierung fehlgeschlagen. Bitte erneut versuchen.',
        'cards.topicSuccess': '{count} visuelle Karten generiert!',
        'cards.partialSuccess': '{success} von {total} Karten generiert (einige Bilder fehlgeschlagen)',
        'cards.imageServiceUnavailable': 'Bilddienst nicht verfügbar. Bitte Bildgenerierung konfigurieren.',
        'cards.batchComplete': 'Alle Bilder generiert!',
        'cards.visualFlashcards': 'Visuelle Karteikarten',
        'cards.visualFlashcardsDesc': 'Mit KI-generierten Bildern lernen',
        'cards.yourLanguage': 'Deine Sprache',
        'cards.targetLanguage': 'Lernsprache',
        
        // Languages
        'lang.de': 'Deutsch',
        'lang.fr': 'Französisch',
        'lang.en': 'Englisch',
        'lang.es': 'Spanisch',
        'lang.it': 'Italienisch',
        'lang.pt': 'Portugiesisch',
        'lang.nl': 'Niederländisch',
        'lang.pl': 'Polnisch',
        'lang.ru': 'Russisch',
        'lang.ja': 'Japanisch',
        'lang.zh': 'Chinesisch',
        'lang.ko': 'Koreanisch'
    }
};

/**
 * Current language code
 */
let currentLanguage = 'en';

/**
 * Set the current language
 * @param {string} langCode - Language code (e.g., 'en', 'de')
 */
export function setLanguage(langCode) {
    currentLanguage = translations[langCode] ? langCode : 'en';
    applyTranslations();
}

/**
 * Get the current language code
 * @returns {string} Current language code
 */
export function getLanguage() {
    return currentLanguage;
}

/**
 * Translate a key with optional parameter substitution
 * @param {string} key - Translation key
 * @param {Object} params - Parameters to substitute (e.g., {target: 'French', native: 'German'})
 * @returns {string} Translated string
 */
export function t(key, params = {}) {
    const dict = translations[currentLanguage] || translations.en;
    let text = dict[key] || translations.en[key] || key;
    
    // Substitute parameters like {target} with actual values
    Object.entries(params).forEach(([param, value]) => {
        text = text.replace(new RegExp(`\\{${param}\\}`, 'g'), value);
    });
    
    return text;
}

/**
 * Apply translations to all elements with data-i18n attribute
 */
export function applyTranslations() {
    document.querySelectorAll('[data-i18n]').forEach(element => {
        const key = element.getAttribute('data-i18n');
        element.textContent = t(key);
    });
    
    // Handle placeholders
    document.querySelectorAll('[data-i18n-placeholder]').forEach(element => {
        const key = element.getAttribute('data-i18n-placeholder');
        element.placeholder = t(key);
    });
    
    // Handle titles
    document.querySelectorAll('[data-i18n-title]').forEach(element => {
        const key = element.getAttribute('data-i18n-title');
        element.title = t(key);
    });
}

/**
 * Get translated language name
 * @param {string} langCode - Language code
 * @returns {string} Translated language name
 */
export function getLanguageName(langCode) {
    return t(`lang.${langCode}`) || langCode.toUpperCase();
}

/**
 * Get translated goal type
 * @param {string} type - Goal type (DAILY, WEEKLY, etc.)
 * @returns {string} Translated goal type
 */
export function getGoalType(type) {
    const typeMap = {
        'DAILY': 'goals.typeDaily',
        'WEEKLY': 'goals.typeWeekly',
        'MONTHLY': 'goals.typeMonthly',
        'YEARLY': 'goals.typeYearly'
    };
    return t(typeMap[type] || type);
}

/**
 * Get translated skill level description
 * @param {string} level - CEFR level (A1-C2)
 * @returns {string} Translated description
 */
export function getSkillLevelDescription(level) {
    return t(`level.${level}`);
}

// Export the i18n object for convenience
export const i18n = {
    t,
    setLanguage,
    getLanguage,
    applyTranslations,
    getLanguageName,
    getGoalType,
    getSkillLevelDescription
};
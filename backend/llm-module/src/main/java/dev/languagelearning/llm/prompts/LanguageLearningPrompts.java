package dev.languagelearning.llm.prompts;

import dev.languagelearning.llm.PromptTemplate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Collection of prompt templates for language learning operations.
 * <p>
 * Templates use placeholders that will be replaced at runtime:
 * <ul>
 *   <li>{nativeLanguage} - User's native language name</li>
 *   <li>{targetLanguage} - Target language being learned</li>
 *   <li>{skillLevel} - User's current skill level (A1-C2)</li>
 *   <li>{topic} - Specific topic for the content</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LanguageLearningPrompts {

    // ==================== System Prompts ====================

    public static final String LANGUAGE_TUTOR_SYSTEM = """
            You are an expert language tutor helping a {nativeLanguage} speaker learn {targetLanguage}.
            The student's current level is {skillLevel} according to the CEFR framework.
            
            Guidelines:
            - Adapt your explanations to the student's level
            - Use {nativeLanguage} for explanations when helpful
            - Provide examples in {targetLanguage} with translations
            - Be encouraging and supportive
            - Focus on practical, everyday usage
            - Correct mistakes gently with explanations
            """;

    // ==================== Learning Plan Prompts ====================

    public static final PromptTemplate CREATE_LEARNING_PLAN = PromptTemplate.of("""
            Create a personalized {targetLanguage} learning plan for a {nativeLanguage} speaker at {skillLevel} level.
            
            Consider the following goals:
            - Daily goal: {dailyGoal}
            - Weekly goal: {weeklyGoal}
            - Monthly goal: {monthlyGoal}
            
            The plan should include:
            1. Recommended daily study time
            2. Key topics to focus on for their level
            3. Suggested learning activities
            4. Milestones to track progress
            5. Tips for staying motivated
            
            Format the response as a structured Markdown document.
            """);

    public static final PromptTemplate ASSESS_SKILL_LEVEL = PromptTemplate.of("""
            Based on the following responses from a {nativeLanguage} speaker learning {targetLanguage}, 
            assess their current CEFR level (A1, A2, B1, B2, C1, or C2).
            
            Student responses:
            {responses}
            
            Provide:
            1. Assessed level with confidence (e.g., "B1 - High confidence")
            2. Strengths observed
            3. Areas for improvement
            4. Recommended focus areas
            
            Be thorough but encouraging in your assessment.
            """);

    // ==================== Lesson Generation Prompts ====================

    public static final PromptTemplate GENERATE_LESSON = PromptTemplate.of("""
            Create a {targetLanguage} lesson for a {nativeLanguage} speaker at {skillLevel} level.
            
            Topic: {topic}
            
            The lesson should include:
            1. Learning objectives (3-5 clear goals)
            2. New vocabulary (8-12 words/phrases with translations and example sentences)
            3. Grammar point explanation (if applicable)
            4. Practice dialogue or text
            5. Cultural note (if relevant)
            6. Exercises for practice
            
            Format as a Markdown document with clear sections.
            Use both {targetLanguage} and {nativeLanguage} appropriately for the student's level.
            """);

    public static final PromptTemplate GENERATE_VOCABULARY_LIST = PromptTemplate.of("""
            Create a vocabulary list for {targetLanguage} learners ({nativeLanguage} speakers) at {skillLevel} level.
            
            Topic: {topic}
            Number of words: {wordCount}
            
            CRITICAL - SKILL LEVEL CONSTRAINTS for {skillLevel}:
            - A1: ONLY use the 100 most common, basic words. Simple concrete nouns (apple, house, cat), basic verbs (be, have, go, eat), numbers 1-20, colors, family members. NO abstract words, NO idioms, NO complex grammar.
            - A2: Use common everyday vocabulary (up to 1000 words). Include basic adjectives, common verbs in present/past tense. Topics: daily routine, weather, shopping, family.
            - B1: Broader vocabulary (up to 2000 words). Can include opinions, feelings, work/travel vocabulary. Some compound words acceptable.
            - B2+: Full range of vocabulary including idioms, nuanced expressions, specialized terms.
            
            STRICTLY match the complexity to {skillLevel} - do NOT include words above this level!
            
            IMPORTANT:
            - The "word" field MUST be in {targetLanguage} (the language being learned)
            - The "translation" field MUST be in {nativeLanguage} (the learner's native language)
            - The "exampleTranslation" field MUST be in {nativeLanguage}
            - All translations must be accurate and natural in {nativeLanguage}
            - Example sentences must use ONLY vocabulary appropriate for {skillLevel}
            
            For each word/phrase include:
            1. word: The word or phrase in {targetLanguage}
            2. pronunciation: IPA or phonetic pronunciation guide
            3. translation: Translation in {nativeLanguage}
            4. partOfSpeech: Part of speech (noun, verb, adjective, etc.)
            5. example: An example sentence using the word in {targetLanguage}
            6. exampleTranslation: Translation of the example in {nativeLanguage}
            7. usageNote: Usage notes or tips (in {nativeLanguage}, optional but helpful)
            
            Return ONLY valid JSON with no additional text:
            {{
              "vocabulary": [
                {{
                  "word": "word in {targetLanguage}",
                  "pronunciation": "phonetic guide",
                  "translation": "translation in {nativeLanguage}",
                  "partOfSpeech": "noun/verb/adjective/etc.",
                  "example": "example sentence in {targetLanguage}",
                  "exampleTranslation": "example translation in {nativeLanguage}",
                  "usageNote": "usage tip in {nativeLanguage}"
                }}
              ]
            }}
            """);

    // ==================== Exercise Generation Prompts ====================

    public static final PromptTemplate GENERATE_TEXT_COMPLETION = PromptTemplate.of("""
            Create a fill-in-the-blank exercise in {targetLanguage} for {nativeLanguage} speakers at {skillLevel} level.
            
            Topic: {topic}
            Number of questions: {questionCount}
            
            CRITICAL - SKILL LEVEL CONSTRAINTS for {skillLevel}:
            - A1: Use ONLY the most basic vocabulary and present tense. Sentences must be very short (3-6 words). Test simple nouns, articles, basic verbs (be, have, like). Example: "I ___ a cat." (have)
            - A2: Simple sentences with common vocabulary. Can test present/past tense, basic prepositions, common adjectives. Sentences 5-8 words.
            - B1: Moderate complexity. Can include different tenses, common conjunctions, comparative forms. Sentences up to 10 words.
            - B2+: Full complexity including subjunctive, complex tenses, idioms, advanced vocabulary.
            
            STRICTLY match complexity to {skillLevel} - A1 learners should NOT see complex grammar!
            
            Format each question as:
            1. The sentence with blank(s) marked as ___
            2. Word bank with options (including correct answer and distractors appropriate for {skillLevel})
            3. The correct answer
            4. Brief explanation in {nativeLanguage} of why this answer is correct
            
            Return as JSON:
            {{
              "exercises": [
                {{
                  "sentence": "...",
                  "wordBank": ["..."],
                  "correctAnswer": "...",
                  "explanation": "..."
                }}
              ]
            }}
            """);

    public static final PromptTemplate GENERATE_DRAG_DROP = PromptTemplate.of("""
            Create word-ordering exercises for learning {targetLanguage}.
            The learner's native language is {nativeLanguage} and their level is {skillLevel}.
            
            Topic: {topic}
            Number of sentences: {sentenceCount}
            
            CRITICAL - SKILL LEVEL CONSTRAINTS for {skillLevel}:
            - A1: Sentences must have 3-5 words ONLY. Use basic SVO structure. Only basic vocabulary (I, you, have, am, is, like, cat, dog, house, water, etc.). NO complex grammar.
            - A2: Sentences 4-6 words. Simple structures with common vocabulary. Can include basic adjectives and adverbs.
            - B1: Sentences 5-8 words. Can include compound sentences, relative clauses basics.
            - B2+: Full complexity allowed.
            
            STRICTLY match complexity to {skillLevel}!
            
            IMPORTANT:
            - The "words" field MUST be in {targetLanguage} (the language being learned) - provide words in CORRECT order, the UI will shuffle them
            - The "translation" field MUST be in {nativeLanguage} (the learner's native language) - this shows what the user needs to build in {targetLanguage}
            - The "explanation" field MUST be in {nativeLanguage} to explain grammar rules
            
            For each exercise provide:
            1. words: An array of words in the CORRECT ORDER forming a valid {targetLanguage} sentence (the frontend will shuffle these for display)
            2. translation: The sentence's meaning in {nativeLanguage} (this helps the user understand what to construct)
            3. explanation: A brief grammar explanation in {nativeLanguage} (e.g., word order rules, verb placement)
            
            Return ONLY valid JSON with no additional text:
            {{
              "exercises": [
                {{
                  "words": ["correct", "word", "order", "in", "{targetLanguage}"],
                  "translation": "translation in {nativeLanguage}",
                  "explanation": "grammar explanation in {nativeLanguage}"
                }}
              ]
            }}
            """);

    public static final PromptTemplate GENERATE_TRANSLATION_EXERCISE = PromptTemplate.of("""
            Create translation exercises for {nativeLanguage} to {targetLanguage} at {skillLevel} level.
            
            Topic: {topic}
            Number of sentences: {sentenceCount}
            
            CRITICAL - SKILL LEVEL CONSTRAINTS for {skillLevel}:
            - A1: Very short sentences (3-5 words). Use ONLY basic vocabulary and present tense. Examples: "I have a cat.", "The house is big.", "I like water."
            - A2: Simple sentences (4-7 words). Present and simple past tense. Common vocabulary. Examples: "Yesterday I went to the store.", "My family is nice."
            - B1: Moderate sentences. Multiple tenses, basic conjunctions, feelings/opinions. Up to 10 words.
            - B2+: Full complexity including idioms, subjunctive, complex structures.
            
            STRICTLY match complexity to {skillLevel}!
            
            For each exercise provide:
            1. Sentence in {nativeLanguage} to translate (complexity matching {skillLevel})
            2. Model answer in {targetLanguage}
            3. Alternative acceptable answers
            4. Key vocabulary/grammar points tested
            
            Return as JSON:
            {{
              "exercises": [
                {{
                  "sourceText": "...",
                  "modelAnswer": "...",
                  "alternatives": ["..."],
                  "keyPoints": ["..."]
                }}
              ]
            }}
            """);

    public static final PromptTemplate GENERATE_LISTENING_EXERCISE = PromptTemplate.of("""
            Create listening comprehension exercises in {targetLanguage} for {nativeLanguage} speakers at {skillLevel} level.
            
            Topic: {topic}
            Number of exercises: {exerciseCount}
            
            For each exercise provide:
            1. A sentence or phrase in {targetLanguage} (will be read aloud via TTS)
            2. The correct transcription (what the student should type after listening)
            3. Translation in {nativeLanguage} for reference
            4. A hint that can help without giving away the answer
            5. Difficulty rating (easy, medium, hard)
            
            Make sentences appropriate for the skill level:
            - A1/A2: Simple, common phrases and short sentences
            - B1/B2: Moderate complexity with some idioms
            - C1/C2: Complex sentences, nuanced expressions
            
            Return as JSON:
            {{
              "exercises": [
                {{
                  "text": "...",
                  "translation": "...",
                  "hint": "...",
                  "difficulty": "easy|medium|hard"
                }}
              ]
            }}
            """);

    public static final PromptTemplate GENERATE_SPEAKING_EXERCISE = PromptTemplate.of("""
            Create speaking/pronunciation exercises in {targetLanguage} for {nativeLanguage} speakers at {skillLevel} level.
            
            Topic: {topic}
            Number of exercises: {exerciseCount}
            
            For each exercise provide:
            1. A sentence or phrase in {targetLanguage} for the student to speak aloud
            2. Translation in {nativeLanguage}
            3. Phonetic guide or pronunciation tips (especially for sounds difficult for {nativeLanguage} speakers)
            4. Common pronunciation mistakes to avoid
            5. Difficulty rating (easy, medium, hard)
            
            Make sentences appropriate for the skill level:
            - A1/A2: Focus on basic pronunciation, simple words and common phrases
            - B1/B2: Include words with challenging sounds, longer sentences
            - C1/C2: Complex intonation patterns, tongue twisters, nuanced expressions
            
            Return as JSON:
            {{
              "exercises": [
                {{
                  "text": "...",
                  "translation": "...",
                  "pronunciationTips": "...",
                  "commonMistakes": ["..."],
                  "difficulty": "easy|medium|hard"
                }}
              ]
            }}
            """);

    // ==================== Roleplay/Scenario Prompts ====================

    public static final PromptTemplate GENERATE_ROLEPLAY_SCENARIO = PromptTemplate.of("""
            Create an interactive roleplay scenario for {targetLanguage} practice.
            Student level: {skillLevel} ({nativeLanguage} speaker)
            
            Scenario: {scenario}
            
            Include:
            1. Scene setting and context
            2. Your role description
            3. Student's role description
            4. Key vocabulary for this scenario
            5. Useful phrases
            6. Conversation starter
            7. Possible conversation directions
            8. Success criteria (what the student should accomplish)
            
            Format as Markdown with clear sections.
            """);

    // ==================== Feedback Prompts ====================

    public static final PromptTemplate EVALUATE_RESPONSE = PromptTemplate.of("""
            Evaluate the following {targetLanguage} response from a {nativeLanguage} speaker at {skillLevel} level.
            
            Exercise: {exercise}
            Student's response: {response}
            Expected answer: {expected}
            
            Provide:
            1. Score (0-100)
            2. Whether the answer is acceptable (boolean)
            3. Specific feedback on what was done well
            4. Corrections needed (if any)
            5. Suggestions for improvement
            6. Encouragement
            
            Return as JSON:
            {{
              "score": 0,
              "acceptable": false,
              "positives": ["..."],
              "corrections": ["..."],
              "suggestions": ["..."],
              "encouragement": "..."
            }}
            """);

    public static final PromptTemplate EVALUATE_PRONUNCIATION = PromptTemplate.of("""
            Evaluate the pronunciation transcription against the expected text.
            Student level: {skillLevel} ({nativeLanguage} speaker learning {targetLanguage})
            
            Expected text: {expected}
            Transcribed speech: {transcription}
            
            Analyze:
            1. Overall accuracy percentage
            2. Words pronounced correctly
            3. Words with issues
            4. Specific pronunciation tips
            5. Common errors for {nativeLanguage} speakers to watch
            
            Return as JSON:
            {{
              "accuracy": 0,
              "correctWords": ["..."],
              "issueWords": [{{"word": "...", "issue": "...", "tip": "..."}}],
              "generalTips": ["..."]
            }}
            """);

    // ==================== Flashcard Prompts ====================

    public static final PromptTemplate GENERATE_FLASHCARDS = PromptTemplate.of("""
            Create vocabulary flashcards for learning {targetLanguage}.
            The learner's native language is {nativeLanguage} and their level is {skillLevel}.
            
            Topic: {topic}
            Number of cards: {cardCount}
            
            CRITICAL - SKILL LEVEL CONSTRAINTS for {skillLevel}:
            - A1: ONLY the most basic, common words. Concrete nouns (cat, dog, house, apple), basic verbs (be, have, go, eat, drink, like), colors, numbers. NO abstract concepts, NO idioms.
            - A2: Common everyday vocabulary. Basic adjectives (big, small, good, bad), time words (today, yesterday), family, food, weather basics.
            - B1: Broader vocabulary including feelings, opinions, work/travel terms. Some compound words.
            - B2+: Full range including idioms, nuanced expressions.
            
            STRICTLY select words appropriate for {skillLevel}!
            
            IMPORTANT: 
            - The "front" field MUST be in {targetLanguage} (the language being learned)
            - The "translation" field MUST be in {nativeLanguage} (the learner's native language)
            - The "exampleTranslation" field MUST be in {nativeLanguage}
            - All translations must be accurate and natural in {nativeLanguage}
            - Example sentences must use ONLY vocabulary appropriate for {skillLevel}
            
            For each flashcard include:
            1. front: A word or phrase in {targetLanguage}
            2. back.translation: The translation in {nativeLanguage}
            3. back.pronunciation: IPA or phonetic pronunciation guide
            4. back.example: An example sentence using the word in {targetLanguage}
            5. back.exampleTranslation: Translation of the example in {nativeLanguage}
            6. mnemonic: A memory tip to help remember the word (in {nativeLanguage})
            7. imagePrompt: A brief description for image generation (in English)
            
            Return ONLY valid JSON with no additional text:
            {{
              "flashcards": [
                {{
                  "front": "word in {targetLanguage}",
                  "back": {{
                    "translation": "translation in {nativeLanguage}",
                    "pronunciation": "phonetic guide",
                    "example": "example sentence in {targetLanguage}",
                    "exampleTranslation": "example translation in {nativeLanguage}"
                  }},
                  "mnemonic": "memory tip in {nativeLanguage}",
                  "imagePrompt": "image description in English"
                }}
              ]
            }}
            """);

    // ==================== Visual Learning Card Prompts ====================

    public static final PromptTemplate GENERATE_VISUAL_VOCABULARY = PromptTemplate.of("""
            Generate vocabulary for visual learning cards in {targetLanguage}.
            The learner's native language is {nativeLanguage} and their level is {skillLevel}.
            
            Topic: {topic}
            Number of words: {wordCount}
            
            CRITICAL - SKILL LEVEL CONSTRAINTS for {skillLevel}:
            - A1: ONLY the most basic, concrete, visual words. Simple nouns like: cat, dog, apple, house, car, water, bread, book, chair, table. NO abstract words.
            - A2: Common everyday objects and simple actions. Add: kitchen items, clothing, body parts, basic actions (run, sleep, eat).
            - B1: Broader vocabulary including less common objects, professions, places.
            - B2+: Can include any visual vocabulary.
            
            STRICTLY select words appropriate for {skillLevel}!
            
            Select words that:
            - Are concrete nouns or actions that can be clearly illustrated with an image
            - Are appropriate for the learner's skill level ({skillLevel})
            - Are related to the given topic
            - Are commonly used in everyday language
            
            IMPORTANT:
            - "nativeWord" MUST be in {nativeLanguage} (the learner's native language)
            - "targetWord" MUST be in {targetLanguage} (the language being learned)
            - "exampleSentence" MUST be in {targetLanguage}
            - "imageDescription" MUST be in English (for image generation AI)
            - Choose words that can be visually represented clearly
            
            For each word provide:
            1. nativeWord: The word in {nativeLanguage}
            2. targetWord: The word in {targetLanguage}
            3. pronunciation: IPA or phonetic pronunciation guide for the {targetLanguage} word
            4. exampleSentence: A simple sentence using the word in {targetLanguage}
            5. imageDescription: A clear, simple description for generating an educational image (in English)
               - Use format: "Simple illustration of [object/action], clean minimalist style, educational"
               - Focus on the core concept, avoid complex scenes
            
            Return ONLY valid JSON with no additional text:
            {{
              "vocabulary": [
                {{
                  "nativeWord": "word in {nativeLanguage}",
                  "targetWord": "word in {targetLanguage}",
                  "pronunciation": "phonetic guide",
                  "exampleSentence": "example in {targetLanguage}",
                  "imageDescription": "Simple illustration of..., clean minimalist style, educational"
                }}
              ]
            }}
            """);
}

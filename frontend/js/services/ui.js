/**
 * UI Module
 * Handles common UI operations: loading, modals, speech synthesis.
 */

import { api } from '../api/client.js';
import { toast } from './toast.js';
import { t } from './i18n.js';

/** Current playing audio state for speakText */
let currentSpeakAudio = null;
let currentSpeakAudioUrl = null;

/**
 * Show loading overlay.
 */
export function showLoading() {
    const el = document.getElementById('loading');
    if (el) el.classList.remove('hidden');
}

/**
 * Hide loading overlay.
 */
export function hideLoading() {
    const el = document.getElementById('loading');
    if (el) el.classList.add('hidden');
}

/**
 * Open modal with content.
 * @param {string} content - HTML content for the modal
 */
export function openModal(content) {
    const modal = document.getElementById('modal-container');
    const modalContent = modal?.querySelector('.modal-content');
    if (modalContent) modalContent.innerHTML = content;
    modal?.classList.remove('hidden');
}

/**
 * Close the modal.
 */
export function closeModal() {
    document.getElementById('modal-container')?.classList.add('hidden');
}

/**
 * Stop any currently playing speech audio.
 */
export function stopSpeakAudio() {
    if (currentSpeakAudio) {
        currentSpeakAudio.pause();
        currentSpeakAudio.currentTime = 0;
        currentSpeakAudio = null;
    }
    if (currentSpeakAudioUrl) {
        URL.revokeObjectURL(currentSpeakAudioUrl);
        currentSpeakAudioUrl = null;
    }
}

/**
 * Speak text using text-to-speech API with play/pause toggle support.
 * @param {string} text - Text to speak
 * @param {string} languageCode - Target language code
 * @param {HTMLElement} btn - The button element that triggered this (optional, for icon updates)
 */
export async function speakText(text, languageCode, btn) {
    if (!text) return;
    
    // If this button's audio is already playing - toggle pause/resume
    if (currentSpeakAudio && btn && btn.dataset.playing === 'true') {
        if (currentSpeakAudio.paused) {
            // Resume playback
            await currentSpeakAudio.play();
            btn.textContent = '⏸️ ' + t('exercises.paused').replace('Paused', 'Pause').replace('Pausiert', 'Pause');
            btn.innerHTML = '⏸️ Pause';
        } else {
            // Pause playback
            currentSpeakAudio.pause();
            btn.innerHTML = '▶️ ' + t('lessons.listen').replace('🔊 ', '');
        }
        return;
    }
    
    // Stop any other audio that might be playing
    stopSpeakAudio();
    
    // Reset all other speak buttons
    document.querySelectorAll('[data-speak-btn="true"]').forEach(otherBtn => {
        if (otherBtn !== btn) {
            otherBtn.innerHTML = '🔊 ' + t('lessons.listen').replace('🔊 ', '');
            otherBtn.dataset.playing = 'false';
        }
    });
    
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '⏳ ' + t('misc.loading').replace('...', '');
    }
    
    toast.info(t('toast.generatingAudio'));
    
    try {
        const audioBlob = await api.speech.synthesize(text, languageCode || 'fr', true);
        currentSpeakAudioUrl = URL.createObjectURL(audioBlob);
        currentSpeakAudio = new Audio(currentSpeakAudioUrl);
        
        currentSpeakAudio.onended = () => {
            if (btn) {
                btn.innerHTML = '🔊 ' + t('lessons.listen').replace('🔊 ', '');
                btn.dataset.playing = 'false';
            }
            currentSpeakAudio = null;
        };
        
        currentSpeakAudio.onerror = () => {
            if (btn) {
                btn.innerHTML = '🔊 ' + t('lessons.listen').replace('🔊 ', '');
                btn.dataset.playing = 'false';
                btn.disabled = false;
            }
            currentSpeakAudio = null;
            toast.error(t('toast.audioPlayFailed'));
        };
        
        await currentSpeakAudio.play();
        
        if (btn) {
            btn.innerHTML = '⏸️ Pause';
            btn.disabled = false;
            btn.dataset.playing = 'true';
            btn.dataset.speakBtn = 'true';
        }
        
        toast.success(t('toast.playingAudio'));
    } catch (error) {
        console.error('Failed to generate speech:', error);
        if (btn) {
            btn.innerHTML = '🔊 ' + t('lessons.listen').replace('🔊 ', '');
            btn.disabled = false;
            btn.dataset.playing = 'false';
        }
        toast.error(t('toast.speechUnavailable'));
    }
}

// Register global functions for use in inline handlers
window.closeModal = closeModal;

export default { showLoading, hideLoading, openModal, closeModal, speakText, stopSpeakAudio };

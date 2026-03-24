/**
 * UI Module
 * Handles common UI operations: loading, modals, speech synthesis.
 */

import { api } from '../api/client.js';
import { toast } from './toast.js';
import { t } from './i18n.js';

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
 * Speak text using text-to-speech API.
 * @param {string} text - Text to speak
 * @param {string} languageCode - Target language code
 */
export async function speakText(text, languageCode) {
    if (!text) return;
    
    toast.info(t('toast.generatingAudio'));
    
    try {
        const audioBlob = await api.speech.synthesize(text, languageCode || 'fr', true);
        const audioUrl = URL.createObjectURL(audioBlob);
        const audio = new Audio(audioUrl);
        audio.play();
        toast.success(t('toast.playingAudio'));
    } catch (error) {
        console.error('Failed to generate speech:', error);
        toast.error(t('toast.speechUnavailable'));
    }
}

// Register global functions for use in inline handlers
window.closeModal = closeModal;

export default { showLoading, hideLoading, openModal, closeModal, speakText };
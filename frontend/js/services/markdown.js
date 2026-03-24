/**
 * Markdown Renderer for Language Learning Platform
 * =================================================
 * Converts Markdown content to HTML for display.
 */

/**
 * Renders Markdown text to HTML.
 * Supports: headings, bold, italic, code, links, lists, blockquotes, 
 * horizontal rules, and code blocks.
 * 
 * @param {string} text - Markdown text to render
 * @returns {string} HTML string
 */
export function renderMarkdown(text) {
    if (!text) return '';
    
    let html = text;
    
    // Escape HTML to prevent XSS (but preserve our markdown conversions)
    html = escapeHtml(html);
    
    // Code blocks (must be before other processing)
    html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (match, lang, code) => {
        return `<pre class="code-block${lang ? ` language-${lang}` : ''}"><code>${code.trim()}</code></pre>`;
    });
    
    // Inline code (must be before other inline processing)
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
    
    // Headings (must be processed line by line to avoid conflicts)
    html = html.replace(/^#### (.*$)/gim, '<h5>$1</h5>');
    html = html.replace(/^### (.*$)/gim, '<h4>$1</h4>');
    html = html.replace(/^## (.*$)/gim, '<h3>$1</h3>');
    html = html.replace(/^# (.*$)/gim, '<h2>$1</h2>');
    
    // Horizontal rules
    html = html.replace(/^---+$/gim, '<hr>');
    html = html.replace(/^\*\*\*+$/gim, '<hr>');
    html = html.replace(/^___+$/gim, '<hr>');
    
    // Blockquotes
    html = html.replace(/^&gt; (.*)$/gim, '<blockquote>$1</blockquote>');
    // Merge consecutive blockquotes
    html = html.replace(/<\/blockquote>\n<blockquote>/g, '\n');
    
    // Bold and italic (order matters)
    html = html.replace(/\*\*\*(.*?)\*\*\*/g, '<strong><em>$1</em></strong>');
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');
    html = html.replace(/___(.*?)___/g, '<strong><em>$1</em></strong>');
    html = html.replace(/__(.*?)__/g, '<strong>$1</strong>');
    html = html.replace(/_(.*?)_/g, '<em>$1</em>');
    
    // Links
    html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');
    
    // Process lists
    html = processLists(html);
    
    // Tables
    html = processTables(html);
    
    // Paragraphs - convert double line breaks to paragraph breaks
    // But avoid wrapping already-processed block elements
    html = html
        .split(/\n\n+/)
        .map(block => {
            block = block.trim();
            if (!block) return '';
            // Don't wrap block elements
            if (/^<(h[1-6]|ul|ol|li|blockquote|pre|hr|table|div)/.test(block)) {
                return block;
            }
            // Convert single line breaks to <br> within paragraphs
            return `<p>${block.replace(/\n/g, '<br>')}</p>`;
        })
        .join('\n');
    
    return html;
}

/**
 * Escapes HTML special characters to prevent XSS.
 */
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}

/**
 * Processes Markdown lists (ordered and unordered).
 */
function processLists(html) {
    const lines = html.split('\n');
    const result = [];
    let inUl = false;
    let inOl = false;
    
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const ulMatch = line.match(/^[\*\-\+] (.+)$/);
        const olMatch = line.match(/^\d+\. (.+)$/);
        
        if (ulMatch) {
            if (!inUl) {
                if (inOl) {
                    result.push('</ol>');
                    inOl = false;
                }
                result.push('<ul>');
                inUl = true;
            }
            result.push(`<li>${ulMatch[1]}</li>`);
        } else if (olMatch) {
            if (!inOl) {
                if (inUl) {
                    result.push('</ul>');
                    inUl = false;
                }
                result.push('<ol>');
                inOl = true;
            }
            result.push(`<li>${olMatch[1]}</li>`);
        } else {
            if (inUl) {
                result.push('</ul>');
                inUl = false;
            }
            if (inOl) {
                result.push('</ol>');
                inOl = false;
            }
            result.push(line);
        }
    }
    
    // Close any open lists
    if (inUl) result.push('</ul>');
    if (inOl) result.push('</ol>');
    
    return result.join('\n');
}

/**
 * Processes Markdown tables.
 */
function processTables(html) {
    const lines = html.split('\n');
    const result = [];
    let inTable = false;
    let tableRows = [];
    let headerProcessed = false;
    
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        
        // Check if line is a table row (starts and ends with |)
        if (line.startsWith('|') && line.endsWith('|')) {
            // Check if this is a separator row (|---|---|)
            if (/^\|[\s\-:]+\|$/.test(line.replace(/\|/g, '|').replace(/[\s\-:]/g, ''))) {
                // This is the separator, skip it but mark header as processed
                headerProcessed = true;
                continue;
            }
            
            if (!inTable) {
                inTable = true;
                tableRows = [];
                headerProcessed = false;
            }
            
            // Parse cells
            const cells = line
                .slice(1, -1) // Remove leading and trailing |
                .split('|')
                .map(cell => cell.trim());
            
            tableRows.push({ cells, isHeader: !headerProcessed && tableRows.length === 0 });
        } else {
            if (inTable) {
                // End of table, render it
                result.push(renderTable(tableRows));
                inTable = false;
                tableRows = [];
                headerProcessed = false;
            }
            result.push(line);
        }
    }
    
    // Handle table at end of content
    if (inTable) {
        result.push(renderTable(tableRows));
    }
    
    return result.join('\n');
}

/**
 * Renders a table from parsed rows.
 */
function renderTable(rows) {
    if (rows.length === 0) return '';
    
    let html = '<table class="markdown-table">';
    
    rows.forEach((row, index) => {
        if (row.isHeader) {
            html += '<thead><tr>';
            row.cells.forEach(cell => {
                html += `<th>${cell}</th>`;
            });
            html += '</tr></thead><tbody>';
        } else {
            if (index === 0) html += '<tbody>';
            html += '<tr>';
            row.cells.forEach(cell => {
                html += `<td>${cell}</td>`;
            });
            html += '</tr>';
        }
    });
    
    html += '</tbody></table>';
    return html;
}

export default { renderMarkdown };
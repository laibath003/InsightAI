
const chunkText = (text, chunkSize = 1000, overlap = 150) => {
    const cleanedText = text.replace(/\s+/g, " ").trim();

    if (!cleanedText) {
        return [];
    }

    const chunks = [];
    let start = 0;

    while (start < cleanedText.length) {
        const end = Math.min(start + chunkSize, cleanedText.length);
        chunks.push(cleanedText.slice(start, end));

        if (end === cleanedText.length) {
            break;
        }

        start = end - overlap;
    }

    return chunks;
};

export { chunkText };
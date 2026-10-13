
import { PDFParse } from "pdf-parse";

const extractText = async (buffer) => {
    const parser = new PDFParse({
        data: new Uint8Array(buffer)
    });

    try {
        const result = await parser.getText();
        const text = result.text.trim();

        if (!text) {
            throw new Error(
                "No readable text found in this PDF. It may be scanned."
            );
        }

        return text;
    } finally {
        await parser.destroy();
    }
};

export { extractText };
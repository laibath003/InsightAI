
import {
    research as researchService,
    combinedResearch as combinedResearchService
} from "../services/research.service.js";

import { streamResearch as streamResearchService } from "../services/streaming-research.service.js";

const research = async (req, res, next) => {
    try {
        const result = await researchService(req.body);
        res.json(result);
    } catch (error) {
        next(error);
    }
};

const combinedResearch = async (req, res, next) => {
    try {
        const result = await combinedResearchService(req.body);
        res.json(result);
    } catch (error) {
        next(error);
    }
};

const streamResearch = async (req, res, next) => {
    try {
        res.status(200);
        res.setHeader("Content-Type", "application/x-ndjson; charset=utf-8");
        res.setHeader("Cache-Control", "no-cache, no-transform");
        res.setHeader("Connection", "keep-alive");
        res.setHeader("X-Accel-Buffering", "no");
        res.flushHeaders();

        const emit = event => {
            if (!res.destroyed && !res.writableEnded) {
                res.write(`${JSON.stringify(event)}\n`);
            }
        };

        await streamResearchService(req.body, emit);

        if (!res.destroyed && !res.writableEnded) {
            res.end();
        }
    } catch (error) {
        if (res.headersSent) {
            console.error("Streaming research failed:", error);

            if (!res.destroyed && !res.writableEnded) {
                res.write(JSON.stringify({
                    type: "error",
                    message: error.status >= 500
                        ? "The AI service is temporarily unavailable. Please try again."
                        : error.message || "Research failed."
                }) + "\n");

                res.end();
            }
        } else {
            next(error);
        }
    }
};

export { research, combinedResearch, streamResearch };

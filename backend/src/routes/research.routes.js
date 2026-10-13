
import express from "express";

import {
    research,
    combinedResearch,
    streamResearch
} from "../controllers/research.controller.js";

const router = express.Router();

// Streaming endpoint
router.post("/stream", streamResearch);

// Existing endpoints remain available
router.post("/", research);
router.post("/both", combinedResearch);

export default router;

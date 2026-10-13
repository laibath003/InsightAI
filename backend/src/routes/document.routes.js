
import express from "express";
import { upload } from "../middleware/upload.middleware.js";
import {
    uploadDocument,
    searchDocument,
    askDocument
} from "../controllers/document.controller.js";

const router = express.Router();

router.post("/upload", upload.single("document"), uploadDocument);
router.post("/:documentId/search", searchDocument);
router.post("/:documentId/ask", askDocument);

export default router;
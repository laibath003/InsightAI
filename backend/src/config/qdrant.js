
import { QdrantClient } from "@qdrant/js-client-rest";
import { env } from "./env.js";

const qdrantClient = new QdrantClient({
    url: env.qdrantUrl,
    apiKey: env.qdrantApiKey
});

const ensureCollection = async () => {
    const collectionName = env.qdrantCollection;

    const result = await qdrantClient.collectionExists(collectionName);

    if (!result.exists) {
        await qdrantClient.createCollection(collectionName, {
            vectors: {
                size: 768,
                distance: "Cosine"
            }
        });

        console.log(`Qdrant collection created: ${collectionName}`);
    } else {
        console.log(`Qdrant collection already exists: ${collectionName}`);
    }

    // Create the payload index required for document filtering.
    const collection = await qdrantClient.getCollection(collectionName);

    if (!collection.payload_schema?.documentId) {
        await qdrantClient.createPayloadIndex(
            collectionName,
            {
                field_name: "documentId",
                field_schema: "keyword"
            },
            {
                wait: true
            }
        );

        console.log("Qdrant documentId index created.");
    } else {
        console.log("Qdrant documentId index already exists.");
    }
};

export { qdrantClient, ensureCollection };
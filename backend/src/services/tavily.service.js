import { env } from "../config/env.js";

const searchWeb = async (query) => {
    const response = await fetch("https://api.tavily.com/search", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            api_key: env.tavilyApiKey,
            query,
            search_depth: "basic",
            max_results: 5,
            include_answer: false
        })
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Tavily API error: ${errorText}`);
    }

    const data = await response.json();

    return data.results.map((result) => ({
        title: result.title,
        url: result.url,
        content: result.content
    }));
};

export { searchWeb };
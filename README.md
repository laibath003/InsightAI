# InsightAI — AI-Powered Research Assistant

InsightAI is an AI-powered research assistant that combines web research and document-based question answering in one Android application. It helps users explore topics, ask questions about uploaded PDF documents, and receive AI-generated answers with relevant source citations.

The application uses a Kotlin-based Android frontend and a Node.js/Express backend, integrating Gemini AI, Tavily web search, and Qdrant vector search to deliver context-aware research responses.

## 🎥 Project Demo

**Watch the InsightAI demonstration video:** [Add your video link here](YOUR_VIDEO_URL_HERE)

The demo showcases the application's interface, AI-powered web research, document uploads, and document-based question answering.

## ✨ Features

- **AI-Powered Web Research:** Ask questions and retrieve relevant information from the web using Tavily Search.
- **PDF Document Upload:** Upload PDF documents and ask questions about their contents.
- **Retrieval-Augmented Generation (RAG):** Combines document retrieval with Gemini AI to generate context-aware answers.
- **Semantic Search:** Uses embeddings and Qdrant vector search to retrieve relevant document chunks based on user queries.
- **Combined Research:** Supports research using web results and uploaded document context.
- **Source Citations:** Displays clickable source links so users can explore the original information.
- **Streaming AI Responses:** Supports streaming research responses for a more responsive experience.
- **New Chat:** Start a fresh conversation without retaining chat history across sessions.
- **Animated Splash Screen:** Features a custom animated introduction using Lottie.
- **MVVM Architecture:** Separates UI, presentation logic, and data operations for better maintainability.

## 🛠️ Tech Stack

### Android Application
- Kotlin
- XML Layouts
- MVVM Architecture
- Android Jetpack Navigation
- ViewModel
- Kotlin Coroutines
- Retrofit
- Hilt for dependency injection
- Lottie animations

### Backend
- Node.js
- Express.js
- REST APIs
- JavaScript ES Modules
- Multer for document uploads
- PDF text extraction

### AI & Retrieval
- Google Gemini AI — AI-generated research answers
- Tavily Search API — Web research
- Embeddings — Document vectorization
- Qdrant — Vector storage and semantic similarity search
- Retrieval-Augmented Generation (RAG)

## 🏗️ Architecture

InsightAI follows a client-server architecture.

```text
Android Application (Kotlin/XML)
            |
            v
    ViewModel (MVVM)
            |
            v
       Repository
            |
            v
    Retrofit API Client
            |
            v
   Node.js / Express Backend
            |
       +----+----------------+
       |                     |
       v                     v
  Tavily Web Search     PDF Processing
                             |
                             v
                      Text Chunking
                             |
                             v
                       Embeddings
                             |
                             v
                       Qdrant Search
                             |
             +---------------+
             |
             v
         Gemini AI
             |
             v
     Research Response
             |
             v
 Android UI with Citations
```

## 🔍 How It Works

### 1. Web Research
The user submits a question through the Android app. The backend retrieves relevant web results using Tavily and uses Gemini AI to generate a research response with supporting sources.

### 2. Document-Based Question Answering
When a user uploads a PDF, the backend extracts its text, divides it into smaller chunks, generates embeddings, and stores vectors in Qdrant for similarity search.

When the user asks a question, the system retrieves relevant document chunks and supplies them as context to Gemini AI to generate an answer grounded in the uploaded material.

### 3. Combined Research
The application can combine relevant web information and uploaded document context to help users investigate a topic from multiple sources.

### 4. Streaming Responses
The backend supports streaming research responses, allowing generated content to be delivered incrementally instead of waiting for the entire response.

## 📂 Project Structure

```text
InsightAI/
├── android/
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/example/insightai/
│   │       │   ├── data/
│   │       │   │   ├── model/
│   │       │   │   ├── remote/
│   │       │   │   └── repository/
│   │       │   ├── di/
│   │       │   ├── domain/
│   │       │   └── ui/
│   │       │       ├── chat/
│   │       │       ├── components/
│   │       │       ├── document/
│   │       │       ├── main/
│   │       │       ├── newchat/
│   │       │       └── splash/
│   │       └── res/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── backend/
│   ├── src/
│   │   ├── config/
│   │   ├── controllers/
│   │   ├── middleware/
│   │   ├── prompts/
│   │   ├── routes/
│   │   ├── services/
│   │   ├── utils/
│   │   └── validators/
│   ├── .env.example
│   ├── package.json
│   └── package-lock.json
│
└── .gitignore
```

## ⚙️ Getting Started

### Prerequisites

Before running the project, ensure you have:

- Android Studio
- Android SDK and an Android emulator or physical device
- JDK compatible with the project's Gradle configuration
- Node.js and npm
- A Google Gemini API key
- A Tavily API key
- A Qdrant instance and its connection details

### 1. Clone the Repository

```bash
git clone https://github.com/laibath003/InsightAI.git
cd InsightAI
```

### 2. Configure the Backend

Navigate to the backend directory:

```bash
cd backend
npm install
```

Create a `.env` file based on the provided example:

```bash
cp .env.example .env
```

On Windows Command Prompt, you can use:

```cmd
copy .env.example .env
```

Add your own API keys and configuration values to `.env`, following the variable names defined in `.env.example` and the backend configuration.

**Never commit your real `.env` file or expose API keys publicly.**

### 3. Start the Backend

Check the scripts in `backend/package.json` and run the appropriate development or start command. For example, if the project defines a `start` script:

```bash
npm start
```

If it defines a development script instead, use the corresponding command.

The backend must be running and its required AI and vector services must be configured before using the research features.

### 4. Configure the Android Application

1. Open the `android` directory in Android Studio.
2. Allow Gradle to synchronize.
3. Ensure the backend base URL in the Android network configuration points to your running backend.
4. For a standard Android emulator connecting to a backend running on the same computer, the host is commonly `10.0.2.2` rather than `localhost`.
5. Run the app on an emulator or compatible Android device.

**Note:** A physical Android device generally needs to reach the backend through the computer's LAN IP address or another reachable backend URL. Network and firewall configuration may also be required.

## 🔐 Environment Variables

The backend uses environment variables to configure external services. Refer to [`backend/.env.example`](backend/.env.example) for the exact variable names required by this project.

Keep real credentials private and use your own keys and service configuration when running the application.

## 🚀 Future Improvements

Potential enhancements include:

- User authentication and personalized research sessions
- Persistent research projects and saved documents
- More document formats
- Improved retrieval evaluation and source relevance
- Expanded error handling and automated testing
- Deployment of the backend to a hosted environment

## 👩‍💻 Developer

**Laiba Tahir**  
Computer Science Graduate | Android Developer

GitHub: [@laibath003](https://github.com/laibath003)


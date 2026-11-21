import { defineConfig } from "orval";

// API client'ı backend'in OpenAPI spec'inden üretilir (ADR-011).
// Üretilen dosyalar elle düzenlenmez: `pnpm api:generate`.
export default defineConfig({
    venyu: {
        input: "../docs/api/openapi.json",
        output: {
            mode: "tags-split",
            target: "src/api/generated",
            schemas: "src/api/generated/model",
            client: "react-query",
            httpClient: "fetch",
            clean: true,
            urlEncodeParameters: true,
            formatter: "prettier",
            override: {
                mutator: { path: "src/api/fetcher.ts", name: "apiFetch" },
            },
        },
    },
});

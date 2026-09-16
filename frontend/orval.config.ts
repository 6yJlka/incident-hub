import { defineConfig } from 'orval'

export default defineConfig({
  incidentHub: {
    input: {
      target: './openapi/incident-hub.json',
    },
    output: {
      mode: 'tags-split',
      target: './src/api/generated/incident-hub.ts',
      schemas: './src/api/generated/model',
      client: 'fetch',
      clean: true,
      override: {
        fetch: {
          includeHttpResponseReturnType: false,
        },
        mutator: {
          path: './src/api/http.ts',
          name: 'apiFetch',
        },
      },
    },
  },
})

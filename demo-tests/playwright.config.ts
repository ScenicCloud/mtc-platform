import { defineConfig, devices } from '@playwright/test'
import path from 'path'
import os from 'os'

export default defineConfig({
  testDir: '.',
  timeout: 30000,
  use: {
    baseURL: 'http://127.0.0.1:5174',
    headless: true,
    launchOptions: {
      // 使用已有的 chromium-1243 完整版（跳过 headless-shell）
      executablePath: path.join(
        os.homedir(),
        'Library/Caches/ms-playwright/chromium-1243/chrome-mac-arm64/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing'
      ),
    },
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})

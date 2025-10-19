// Increase Karma timeouts to prevent ChromeHeadless SIGKILL warnings
config.set({
    // Time to wait for browser to connect (default: 2000ms)
    captureTimeout: 60000,

    // Time to wait for browser to reconnect (default: 2000ms)
    browserDisconnectTimeout: 10000,

    // How long to wait for browser to reconnect after disconnect (default: 2000ms)
    browserNoActivityTimeout: 60000,

    // Custom launcher to prevent hanging
    customLaunchers: {
        ChromeHeadlessCI: {
            base: 'ChromeHeadless',
            flags: [
                '--no-sandbox',
                '--disable-gpu',
                '--disable-dev-shm-usage',
                '--disable-software-rasterizer',
                '--disable-extensions'
            ]
        }
    }
});

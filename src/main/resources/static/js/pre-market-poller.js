function showHidePreMarketOptions() {
    const selectElement = document.getElementById('priceMilestone');
    const options = selectElement.options;

    // Get current time in New York
    const now = new Date();
    const nyTimeString = now.toLocaleString('en-US', { timeZone: 'America/New_York' });
    const nyTime = new Date(nyTimeString);
    const day = nyTime.getDay(); // 0 (Sun) - 6 (Sat)
    const hour = nyTime.getHours();
    const minute = nyTime.getMinutes();

    // Check if it's Mon-Fri (1-5) and between 8:00 - 9:30 AM (NY time)
    const isWeekday = day >= 1 && day <= 5;
    const isTimeInRange = hour === 8 || (hour === 9 && minute < 30);

    if (!(isWeekday && isTimeInRange)) {
        for (let i = 0; i < options.length; i++) {
            const optionValue = options[i].value;
            if (optionValue.startsWith('GAP_')) {
                options[i].style.display = 'none';
            }
        }
    }
}

// Call this function every five minutes to update visibility of pre-market options.
setInterval(showHidePreMarketOptions, 1000 * 60 * 5);

window.onload = showHidePreMarketOptions;

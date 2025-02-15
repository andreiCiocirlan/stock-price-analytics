let previousVisibility = null;

function updatePremarketOptionVisibility(isVisible) {
    // Check if visibility hasn't changed
    if (previousVisibility === isVisible) return;

    previousVisibility = isVisible;

    let selectElement;
    try {
        selectElement = document.getElementById('priceMilestone');
        if (!selectElement) throw new Error("Select element not found");
    } catch (e) {
        console.error(e);
        return;
    }
    const preMarketOptions = selectElement.querySelectorAll('.pre-market-option');

    preMarketOptions.forEach(option => {
        option.style.display = isVisible ? "" : "none";
    });
}

function checkAndSetPremarketVisibility() {
    let now = new Date();
    let nytzString = now.toLocaleString("en-US", { timeZone: "America/New_York" });
    let nytzDate = new Date(nytzString);

    // Check if within specified time frame
    let weekday = (nytzDate.getDay() >= 1 && nytzDate.getDay() <= 5),
        hours = ((nytzDate.getHours() === 8) || ((nytzDate.getHours() === 9 && nytzDate.getMinutes() < 30)));

    updatePremarketOptionVisibility(weekday && hours);
}

// Call this function every minute to update visibility of pre-market options.
setInterval(checkAndSetPremarketVisibility, 1000 * 60 * 1);

window.onload = checkAndSetPremarketVisibility;
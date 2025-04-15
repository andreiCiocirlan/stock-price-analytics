var stompClient = null;

function connect() {
    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);

        // Subscribe to desktop notifications
        stompClient.subscribe('/topic/desktop-notification', function (message) {
            const notification = JSON.parse(message.body);
            showAlert(notification.title, notification.message);
        });

        // Subscribe to stock updates
        stompClient.subscribe('/topic/stock-updates', function (message) {
            updateStockPerformanceChartCurrentTimeframe();
        });
    });
}

function sendAlert(message) {
    stompClient.send("/app/alert", {}, message);
}

function showAlert(title, message) {
    if (Notification.permission === "granted") {
        var notification = new Notification(title, { body: message });
    } else if (Notification.permission !== "denied") {
        console.log("Notification permission is not granted or denied yet.  Please click the button.");
         Notification.requestPermission().then(function (permission) {
            if (permission === "granted") {
                console.log("Notification permission granted.");
                var notification = new Notification(title, { body: message });
            } else {
                console.log("Notification permission denied.");
            }
        });
    }
}

$(function () {
    connect();
});

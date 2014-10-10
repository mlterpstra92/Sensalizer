$(document ).ready(function() {
    $("#graphModForm").hide();
    var ratio = $("#canvasContainer").height() / $("#canvasContainer").width();
    //Get the context of the canvas element we want to select
    var c = $('#feedGraph');
    var ct = c.get(0).getContext('2d');
    var ctx = document.getElementById("feedGraph").getContext("2d");
    /*************************************************************************/
    //Run function when window resizes
    $(window).resize(respondCanvas);
    var steps = 3;
    var max = 100;
    var options = {
        responsive: true,
        animation: false,
        barValueSpacing: 5,
        barDatasetSpacing: 1,
        showTooltips: true,
        label: {format: 'shortTime'}

    };

    var chart;

    function respondCanvas() {
        c.attr('width', $("#canvasContainer").width());
        c.attr('height', $("#canvasContainer").width() * (ratio));
        //Call a function to redraw other content (texts, images etc)
        //chart = new Chart(ct).Line(data, options);
    }

    //Initial call
    respondCanvas();

    function padLeft(nr, n, str) {
        return Array(n - String(nr).length + 1).join(str || '0') + nr;
    }

    JSON.stringify = JSON.stringify || function (obj) {
        var t = typeof (obj);
        if (t != "object" || obj === null) {
            // simple data type
            if (t == "string") obj = '"' + obj + '"';
            return String(obj);
        }
        else {
            // recurse array or object
            var n, v, json = [], arr = (obj && obj.constructor == Array);
            for (n in obj) {
                v = obj[n];
                t = typeof(v);
                if (t == "string") v = '"' + v + '"';
                else if (t == "object" && v !== null) v = JSON.stringify(v);
                json.push((arr ? "" : '"' + n + '":') + String(v));
            }
            return (arr ? "[" : "{") + String(json) + (arr ? "]" : "}");
        }
    };

    //Apparently, we eat click events, so use event delegation
    $(this).on('click', 'button', function (e) {
        e.preventDefault();

        if (e.currentTarget.id == "selectFeed") {
            var feedID = $(this).parent().parent().find('td')[0].innerHTML.trim();
            var apiKey = $(this).parent().parent().find('td')[3].innerHTML.trim();

            $.ajax({
                type: "POST",
                url: "triggerFeed",
                data: {feedid: feedID, apikey: apiKey},
                success: success
            });
            // Use SockJS
            Stomp.WebSocketClass = SockJS;

            var RabbitMQIP = "54.171.108.54"
            var username = "guest",
                password = "guest",
                vhost    = "/",
                url      = 'http://' + RabbitMQIP + ':15674/stomp',
                queue    = "/topic/sensalizer.#"; // To translate mqtt topics to
            // stomp we change slashes
            // to dots
            var console;

            function on_connect() {
                console += 'Connected to RabbitMQ-Web-Stomp<br />';
                console.log(client);
                client.subscribe(queue, on_message);
            }

            function on_connection_error() {
                console.innerHTML += 'Connection failed!<br />';
            }

            function on_message(m) {
                console.log('Received:' + m);
                output.innerHTML += m.body + '<br />';
            }

            var ws = new SockJS(url);
            var client = Stomp.over(ws);
            client.heartbeat.outgoing = 0;
            client.heartbeat.incoming = 0;

            window.onload = function () {
                console = document.getElementById("console");
                // Connect
                client.connect(
                    username,
                    password,
                    on_connect,
                    on_connection_error,
                    vhost
                );
            }

            function success(){
                console.log("cool");
            }
        }
    });
});
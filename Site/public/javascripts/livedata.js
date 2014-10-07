$(document ).ready(function(){

    var data = {
        labels: ["January", "February", "March", "April", "May", "June", "July"],
        datasets:
            [
                {
                    label: "My First dataset",
                    fillColor: "rgba(220,220,220,0.2)",
                    strokeColor: "rgba(220,220,220,1)",
                    pointColor: "rgba(220,220,220,1)",
                    pointStrokeColor: "#fff",
                    pointHighlightFill: "#fff",
                    pointHighlightStroke: "rgba(220,220,220,1)",
                    data: [65, 59, 80, 81, 56, 55, 40]
                },
                {
                    label: "My Second dataset",
                    fillColor: "rgba(151,187,205,0.2)",
                    strokeColor: "rgba(151,187,205,1)",
                    pointColor: "rgba(151,187,205,1)",
                    pointStrokeColor: "#fff",
                    pointHighlightFill: "#fff",
                    pointHighlightStroke: "rgba(151,187,205,1)",
                    data: [28, 48, 40, 19, 86, 27, 90]
                }
            ]
    };
    var ratio = $("#canvasContainer").height()/$("#canvasContainer").width();
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
        responsive : true,
        animation: false,
        barValueSpacing : 5,
        barDatasetSpacing : 1,
        showTooltips: true

        /*animation : false,
         responsive : true/*,
         animationEasing: 'easeOutQuart',
         animationSteps: 500,
         segmentShowStroke: true,
         segmentStrokeColor: 'FFF',
         scaleOverride: true,
         scaleSteps: steps,
         scaleStepWidth: Math.ceil(max / steps),
         scaleStartValue: 0*/
    };

    var chart;
    function respondCanvas() {
        c.attr('width', $("#canvasContainer").width());
        c.attr('height', $("#canvasContainer").width()*(ratio));
        //Call a function to redraw other content (texts, images etc)
        //chart = new Chart(ct).Line(data, options);
    }

    //Initial call
    respondCanvas();

    function padLeft(nr, n, str){
        return Array(n-String(nr).length+1).join(str||'0')+nr;
    }

    //Apparently, we eat click events, so use event delegation
    $(this ).on('click', 'button', function(e){
        e.preventDefault();

        //If a feed management is called, perform those actions
        //That is, load the feed the user clicks on using AJAX
        if (e.currentTarget.id == "selectFeed"){
            //this     td       tr        first td      its data without spaces
            var feedID = $(this).parent().parent().find('td')[0].innerHTML.trim();
            var apiKey = $(this ).parent().parent().find('td')[3].innerHTML.trim();
            xively.setKey( apiKey );
            $.ajax({
                url: "feed/"+feedID,
                method: 'GET',
                content: 'json',
                success: function (d) {
                    d.datasets = d.datasets[0];

                    for (var i = 0; i < d.datasets.length; ++i)
                        d.datasets[i] = d.datasets[i][0];

                    var now = new Date();
                    now.setSeconds(now.getSeconds() - 4);
                    var hours, minutes, seconds;
                    hours = padLeft(now.getUTCHours(), 2);
                    minutes = padLeft(now.getUTCMinutes(), 2);
                    seconds = padLeft(now.getUTCSeconds(), 2);
                    d.labels = [];

                    d.labels.push(hours + ":" + minutes + ":" + seconds);
                    for (var i = 0; i < d.datasets.length - 1; ++i)
                        d.labels.push("");

                    //add some colors, maximum of four. Add more if more datasets
                    var colors = ["rgba(200,0,0,1)","rgba(0,200,0,1)","rgba(0,0,200,1)","rgba(200,200,200,1)"]
                    for (var i = 0; i < d.datasets.length; ++i) {
                        d.datasets[i].pointColor = colors[i];
                        d.datasets[i].strokeColor = colors[i];
                    }

                    var chart = new Chart(ct).Line(d, options);
                    xively.feed.subscribe(feedID, function(event, data){
                        var emptyIndex = d.labels.indexOf("");
                        if (emptyIndex != -1)
                            d.labels.splice(emptyIndex, 1);
                        var label = data.updated.split('T' )[1].split('.' )[0];
                        var vals = [];
                        for (var i = 0; i < data.datastreams.length; ++i)
                            vals.push(data.datastreams[i].current_value);

                        chart.addData(vals, label);
                        chart.update();
                    });

                },
                error: function (xhr, ajaxOptions, thrownError) {
                    alert(xhr.status);
                    alert(thrownError);
                }
            });
        }
    });
});

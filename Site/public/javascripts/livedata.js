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
    }

    function respondCanvas() {
        c.attr('width', $("#canvasContainer").width());
        c.attr('height', $("#canvasContainer").width()*(ratio));
        //Call a function to redraw other content (texts, images etc)
        //new Chart(ct).Line(data, options);
    }

    //Initial call
    respondCanvas();

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

                    if ( d.datasets.length != 0)
                        new Chart ( ct ).Line ( d, options );

                    d.labels = [];
                    xively.feed.subscribe(feedID, function(event, data){
                        console.log(data);
                        var label = data.updated.split('T' )[1].split('.' )[0];

                        d.labels.push(label);
                        if (d.datasets[0 ].label != data.datastreams[0 ].id)
                            data.datastreams = data.datastreams.reverse();
                        for (var i = 0; i < d.datasets.length; ++i) {
                            d.datasets[ i ].label = label ;
                            d.datasets[ i ].data.push ( data.datastreams[ i ].current_value );
                            if (d.datasets[i ].data.length > 10 && d.labels.length > 10)
                            {
                                d.datasets[i ].data = d.datasets[i ].data.slice( 1) ;
                                d.labels = d.labels.slice( 1);
                            }
                        }
                        new Chart(ct ).Line(d, options);
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

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE xsl:stylesheet [
        <!ENTITY mdash "&#151;">
        <!ENTITY nbsp "&#160;">
        <!ENTITY larr "&#8592;">
        <!ENTITY rarr "&#8594;">
        <!ENTITY darr "&#8595;">
        ]>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html" encoding="utf-8" indent="yes" />

    <xsl:param name="version"/>
    <xsl:param name="app-file"/>
    <xsl:param name="whenBuilt"/>

    <xsl:template match="/build">
        <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html></xsl:text>
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
                <meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width,initial-scale=1"/>
                <link href="favicon.ico" rel="shortcut icon"/>
                <link href="css/bootstrap.min.css" rel="stylesheet"/>
                <link href="css/bootstrap-responsive.min.css" rel="stylesheet"/>
                <link href="css/docs.css" rel="stylesheet"/>
                <style>
                    html, body {
                    height: 100%;
                    }

                    .container {
                    height: 100%;
                    }

                    footer {
                    clear: both;
                    position: relative;
                    z-index: 10;
                    height: 3em;
                    margin-top: -10em;
                    background-color: rgb(245, 245, 245);
                    text-align: center;
                    padding: 15px 0 10px 0;
                    }

                    .centered {
                    text-align: center;
                    }

                    .top-padded {
                    padding-top: 30px;
                    }
                </style>
            </head>

            <body>

                <header class="header clearfix">
                    <div class="container">
                        <a class="brand">HelloApp</a>
                    </div>
                </header>


                <div class="container">

                    <div class="row">
                        <div class="span4">
                            <section>
                                <h4>
                                    HelloApp для Android
                                </h4>

                                <p>
                                    <dl class="dl-horizontal">
                                        <dt>Версия</dt>
                                        <dd><xsl:value-of select="$version"/></dd>
                                        <dt>Собрана</dt>
                                        <dd><xsl:value-of select="$whenBuilt"/></dd>
                                    </dl>
                                </p>
                                <p class="centered top-padded">
                                    <a class="btn btn-large" href="{$app-file}">
                                        <i class="icon-download-alt"></i>
                                        Скачать
                                    </a>
                                </p>
                            </section>
                        </div>
                    </div>
                </div>


                <footer>
                    <div class="copyright">Microcosmus 2013</div>
                </footer>

            </body>
        </html>


    </xsl:template>

</xsl:stylesheet>

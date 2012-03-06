function loadMenu(title) {
	var menu = window.document.getElementById("menu");
	if (menu) {
		menu.style.marginLeft = "30px";
		menu.innerHTML = 
			"<table cellpadding=10>" +
				"<tr>" +
					"<td><a href='../index.html'><img src='../images/logo-small.png' alt='Diver'></img></a></td>" +
					"<td><h1>" + title + "</h1>" +
						"<a href='../index.html'>HOME</a> | <a href='../features.html'>FEATURES</a> | <a href='../research.html'>RESEARCH</a> | <a href='Diver.html'>DOCS</a> | <a href='../tutorials.html'>TUTORIALS</a> <br> " + 
						"<a href='../download.html'>DOWNLOAD</a> | <a href='../new.html'> NEW </a> | <a href='http://sourceforge.net/projects/diver'>PROJECT</a></td>" +
				"</tr>" +
			"</table> <hr>";
		
	}
	
	var footer = window.document.getElementById("footer");
	if (footer) {
		footer.innerHTML =  
 "<p class='credits'>Dynamic Interactive Tools for Reverse Engineering (Diver)<br>is a research project of" + 
 "The University of Victoria's " + 
 "<a href='http://www.thechiselgroup.org'>Computer Human Interaction &amp; Software Engineering Lab</a>" + 
 "<br>Funded by <a href='http://drdc-rddc.gc.ca'>Defence Research and Development Canada</a> under contract W7701-82702/001/QCA.</p>" + 
 "<br> <div style='height: 60px;'>" +
 "<div style='position:relative; left:15px'><a href='http://www.thechiselgroup.org'><img src='images/chisel.gif' alt='the CHISEL group'></a></div> " +
 "<div style='position:relative; left:302px; top:-50px; height:62px;'><a href='http://www.drdc-rddc.gc.ca'><img src='images/drdc-logo.jpg' alt='Defence Research and Development Canada'></a></div> " +
 "<span style='position:relative; left:610px; top:-102px'><a href='http://sourceforge.net/'><img src='http://sflogo.sourceforge.net/sflogo.php?group_id=169205&amp;type=2'" + 
 "alt='SourceForge.net Logo' height='37' width='125'></a></div></div>";
	}
}

function urlOf(file) {
	return 'http://diver.sf.net/' + file;
}
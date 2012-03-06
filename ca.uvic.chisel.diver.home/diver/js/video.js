function showVideo(videoURL, vwidth, vheight) {
	if (!videoURL) return;
	var top = 0;
	var width = 0;
	var height = 0;
	if (navigator.appName.indexOf("Microsoft") != -1) {
		top = document.documentElement.scrollTop;
		height = document.documentElement.clientHeight;
		width = document.documentElement.clientWidth;
	} else {
		top = window.pageYOffset;
		height = window.innerHeight;
		width=document.innerWidth;
	}
	
	
	var pictureArea = window.document.createElement("div");
	if (pictureArea) {
		pictureArea.id = "glass";
		pictureArea.innerHTML = "&nbsp;";
		pictureArea.style.position = "absolute";
		pictureArea.style.left = "0px";
		pictureArea.style.top = top +"px";
		pictureArea.style.width = "100%";
		pictureArea.style.height = "100%";
		pictureArea.style.opacity = ".4";
		pictureArea.style.filter = "alpha(opacity=40)";
		pictureArea.style.display = "inline";
		pictureArea.style.backgroundColor = "#555555";
		pictureArea.style.visibility = "visible";
		document.body.appendChild(pictureArea);
	}
		
	var popup = window.document.createElement("div");
	//make it fit
	

	if (popup) {
		height = height-40;
		width = width-20;
		var hdiff = vheight - height;
		var wdiff = vwidth - width;
		var aspect = 1;
		if (wdiff > hdiff) {
			if (wdiff > 0) {
				aspect = width/vwidth; 
			}
		} else {
			if (hdiff > 0) {
				aspect = height/vheight;
			}
		}
		popup.vwidth = vwidth;
		popup.vheight = vheight;
		popup.fullscreen = false;
		popup.resizewidth = vwidth*aspect;
		popup.resizeheight = vheight*aspect;
		popup.id = "video";
		popup.style.position = "absolute";
		popup.style.top = (top + ((height/2)-(popup.resizeheight/2)) - 20) + "px";
		popup.style.left = (width/2 - popup.resizewidth/2) + "px";
		popup.centertop = popup.style.top;
		popup.centerleft = popup.style.left;
		popup.style.border = "solid";
		popup.style.borderWidth = "1px";
		popup.style.textAlign = "right";
		popup.style.backgroundColor = "#eeeeee";
		popup.innerHTML =
			"<table>" +
			"<tr><td>" +
			"<OBJECT CLASSID='clsid:D27CDB6E-AE6D-11cf-96B8-444553540000' id='vwin' WIDTH='"+popup.resizewidth+"' HEIGHT='"+popup.resizeheight+ "'" +
			"CODEBASE='http://active.macromedia.com/flash5/cabs/swflash.cab#version=7,0,0,0'>" +
			"<PARAM NAME=movie VALUE='"+videoURL+"'>"+
			"<PARAM NAME=play VALUE=true>" +
			"<PARAM NAME=loop VALUE=false>" +
			"<PARAM NAME=wmode VALUE=transparent>" +
			"<PARAM NAME=quality VALUE=low>" +
//			"<EMBED SRC='"+videoURL+"' WIDTH='"+vwidth+"' HEIGHT='"+vheight+"'" +
//			"quality=low loop=false wmode=transparent " + 
//			"TYPE='application/x-shockwave-flash' " + 
//			"PLUGINSPAGE='http://www.macromedia.com/shockwave/download/index.cgi?P1_Prod_Version=ShockwaveFlash'>"
//			"</EMBED>" +
			"</OBJECT>" +
			
			"</td></tr>" +
			"<tr>" +
			"<td><small><a id='togglefullscreen' onclick='toggleFullScreen()' style='cursor:pointer; font-weight:bold'>[+] FULL SIZE</a></small>" +
			" | <small><a onclick='closevideo()' style='cursor:pointer; font-weight:bold'>[X] CLOSE</a></td>" +
			"</tr>" +
			" </table>";
//		popup.innerHTML = 
//			" <table>" +
//			"  <tr> <td><img src='" + imageURL + "'><td><tr>" +
//			"  <tr> <td><small><div onclick='closeshot()' style='cursor:pointer; font-weight:bold'> [X] CLOSE</div></small></td></tr>" +
//			" </table>";
		document.body.appendChild(popup);
	}
}

function toggleFullScreen() {
	var popup = window.document.getElementById("video");
	var vwin = window.document.getElementById("vwin");
	var toggle = window.document.getElementById("togglefullscreen");
	if (popup && vwin && toggle) {
		if (popup.fullscreen) {
			popup.fullscreen = false;
			popup.style.top = popup.centertop;
			popup.style.left = popup.centerleft;
			vwin.width = popup.resizewidth;
			vwin.height = popup.resizeheight;
			toggle.innerHTML="[+] FULL SIZE";
		} else {
			popup.fullscreen = true;
			popup.style.top = 0;
			popup.style.left = 0;
			vwin.width = popup.vwidth;
			vwin.height = popup.vheight;
			toggle.innerHTML="[-] FIT TO WINDOW";
		}
	}
	
}

function closevideo() {
	var pictureArea = window.document.getElementById("glass");
	if (pictureArea) {
		document.body.removeChild(pictureArea);
	}
	var popup = window.document.getElementById("video");
	if (popup) {
		document.body.removeChild(popup);
	}
}
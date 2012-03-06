var dTop = 0;
var dLeft = 0;
function screenshot(imageURL) {
	var image = new Image();
	image.src = imageURL;
	if (!image) return;
	//make sure that they get loaded... sometimes the browser won't do it
	image.width; image.height;
	var top = 0;
	var height = 600;
	if (navigator.appName.indexOf("Microsoft") != -1) {
		top = document.documentElement.scrollTop;
		height = document.documentElement.clientHeight;
	} else {
		top = window.pageYOffset;
		height = window.innerHeight;
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
		pictureArea.onclick = closeshot;
		document.body.appendChild(pictureArea);
	}
		
	var popup = window.document.createElement("div");
	if (popup) {
		imageHeight = 20;
		imageWidth = 20;
		popup.id = "popup";
		popup.style.position = "absolute";
		dTop = (top + ((height/2)-(imageHeight/2)) - 20);
		popup.style.top = dTop + "px";
		dLeft = (400 - imageWidth/2);
		popup.style.left = dLeft + "px";
		popup.style.border = "solid";
		popup.style.borderWidth = "1px";
		popup.style.textAlign = "right";
		popup.style.backgroundColor = "#eeeeee";
		popup.innerHTML = "<img src='images/ajax-loader.gif'/>";
//		popup.innerHTML = 
//			" <table>" +
//			"  <tr> <td><img src='" + imageURL + "'><td><tr>" +
//			"  <tr> <td><small><div onclick='closeshot()' style='cursor:pointer; font-weight:bold'> [X] CLOSE</div></small></td></tr>" +
//			" </table>";
		document.body.appendChild(popup);
		setTimeout(function(){animateImage(image); image=null;}, 0);
	
	}
}

var tries = 0;
var step = 0;
var steps = 20;
var dwidth = 0;
var dheight = 0;
function animateImage(image) {
	if (image.width == 0 || image.height == 0) {
		if (tries < 50) {
			setTimeout(function(){animateImage(image); image=null;}, 2000);
			return;
		} else {
			tries = 0;
			return;
		}
	}
	tries = 0;
	var pictureArea = document.getElementById("glass");
	if (pictureArea) {
		var top = 0;
		var height = 600;
		if (navigator.appName.indexOf("Microsoft") != -1) {
			top = document.documentElement.scrollTop;
			height = document.documentElement.clientHeight;
		} else {
			top = window.pageYOffset;
			height = window.innerHeight;
		}
		var popup = document.getElementById("popup");
		if (popup) {
			if (dwidth >= image.width || step >= steps) {
				step = 0;
				dwidth = 0;
				dheight = 0;
				popup.style.width = "auto";
				popup.style.height = "auto";
				popup.innerHTML = 
					" <table>" +
					"  <tr> <td><img src='" + image.src + "'><td><tr>" +
					"  <tr> <td><small><div onclick='closeshot()' style='cursor:pointer; font-weight:bold'> [X] CLOSE</div></small></td></tr>" +
					" </table>";
			} else {
				var wInc = image.width/steps;
				var hInc = image.height/steps;
				if (step == 0) {
					dwidth = 0;
					dheight = 0;
				}
				dwidth = dwidth + wInc;
				dheight = dheight + hInc;
				dTop = dTop - (hInc/2);
				dLeft = dLeft - (wInc/2);
				step = step + 1;
				popup.style.width = dwidth + "px";
				popup.style.height = dheight + "px";
				popup.style.top = dTop + "px";
				popup.style.left = dLeft + "px";
				setTimeout(function(){animateImage(image); image=null;}, 0);
			}
		}
	}
}

function closeshot() {
	var pictureArea = window.document.getElementById("glass");
	if (pictureArea) {
		document.body.removeChild(pictureArea);
	}
	var popup = window.document.getElementById("popup");
	if (popup) {
		document.body.removeChild(popup);
	}
}
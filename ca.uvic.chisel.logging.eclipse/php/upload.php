<?php
function quitNow($message) {
	echo "Status: 501 Not Implemented\n\n";
	echo $message;
	exit(1);
}

header('Content-type: text/plain');


$category = $_POST["CATEGORY"];
$uid = $_POST["USER"];
$kind = $_POST["KIND"];

if (!$category||!$uid) {
	quitNow("Bad category or user id");
}

if ($kind != "workbench-log") {
	quitNow("Unknown kind $kind");	
}

//make sure that the upload directory
//exists
$uploadDir = 'uploads/' . $category . '/' . $uid;
if (!is_dir($uploadDir)) {
	if (!mkdir($uploadDir, 0777, true)) {
		quitNow("Could not create upload directory");
	}
}

//now, get the file
if ($_FILES["WorkbenchLogger"]["error"] > 0) {
	quitNow("Error retrieving file: " . $_FILES["WorkbenchLogger"]["error"]);
}
$fileType = $_FILES["WorkbenchLogger"]["type"];
if ($fileType != "application/zip"
	&& $fileType != "application/x-zip-compressed") {
	quitNow("Unsupported file type: " . $_FILES["WorkbenchLogger"]["type"]);
}
if (!move_uploaded_file($_FILES["WorkbenchLogger"]["tmp_name"],
	$uploadDir . "/" . $_FILES["WorkbenchLogger"]["name"]) ) {
	quitNow("Could not move file " . $_FILES["WorkbenchLogger"]["name"]);
}


echo "Status: 200 Success";
?>
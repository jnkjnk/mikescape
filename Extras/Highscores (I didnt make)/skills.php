<?php
$Location = 'Highscores &gt; Skills';
include('includes/header.php');
?>
<?
if($_GET['type'] != "0" && $_GET['type'] != "" && $_GET['type'] != "PK") {
	showPlayers($_GET['type']);
}
?>
<?php include('includes/footer.php'); ?>
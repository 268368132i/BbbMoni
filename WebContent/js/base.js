/**
 * 
 */
function loadList(node,recClass){

	if (node.querySelector("[name='list']")!=null){
		node.querySelector("[name='list']").remove();
		return;
	}

	var div = document.createElement('div');
	div.innerHTML="<img src=\"img/wait.png\"/>";
	node.appendChild(div);
	
	div.setAttribute('name','list');
	fetch('List/?recs=' + recClass)
		.then(resp => {
			return resp.text();
		})
		.then(text => {
			div.innerHTML=text;
			});
	
 }
 
 function sendRecordingCmd(node,cmd,arg){
 	fetch('cmd?comm='+cmd+'&recordings='+arg)
 		.then(resp =>{
 			return resp.json();
 		})
 		.then(data=>{
 			console.log(data);
 			if(data.result === 'SUCCESS' || data.status === 'true'){
 				node.remove();
 			} else {
 				window.alert(data.result);
 			}
 		});
 }
 
 function sendMeetingCmd(node,cmd,arg){
 	fetch('cmd?comm='+cmd+'&'+arg)
 		.then(resp =>{
 			return resp.json();
 		})
 		.then(data=>{
 			console.log(data);
 			if(data.result === 'SUCCESS' || data.status === 'true'){
 				node.remove();
 			} else {
 				window.alert(data.result);
 			}
 		});
 }
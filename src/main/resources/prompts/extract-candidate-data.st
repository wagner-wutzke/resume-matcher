Extract candidate information from the resume below. Return exactly one JSON object and no
Markdown or surrounding text. Use only facts stated in the resume. Use an empty string for
unavailable scalar values and an empty array for experiences when none are stated. Do not
calculate yearsOfExperience; it will be derived from experience periods separately.
Use this exact shape:
{"email":"","fullname":"","country":"","city":"","position":"",
"skills":"","allocation":"","yearsOfExperience":"","experiences":[
{"title":"","company":"","period":"","responsibilities":"","achievements":""}]}

Resume:
%s

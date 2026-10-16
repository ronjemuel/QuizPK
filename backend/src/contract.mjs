export class ApiError extends Error {
  constructor(status, code) { super(code); this.status = status; this.code = code; }
}
export const normalize = value => value.normalize('NFKC').trim().replace(/\s+/gu, ' ').toLowerCase();
const text = (value, min, max, code) => {
  if (typeof value !== 'string' || value.trim().length < min || value.trim().length > max || /[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f]/u.test(value)) throw new ApiError(400, code);
  return value.trim();
};
export function validateRequest(input) {
  const allowed = ['topic','difficulty','questionCount','timerSeconds','additionalInstructions'];
  if (!input || Array.isArray(input) || typeof input !== 'object' || Object.keys(input).some(k => !allowed.includes(k))) throw new ApiError(400,'INVALID_REQUEST');
  const topic = text(input.topic,3,120,'INVALID_TOPIC');
  if (!/[\p{L}\p{N}]/u.test(topic)) throw new ApiError(400,'INVALID_TOPIC');
  if (!['Easy','Normal','Hard','Expert'].includes(input.difficulty) || ![5,10,15,20].includes(input.questionCount) || ![10,15,20,30].includes(input.timerSeconds)) throw new ApiError(400,'INVALID_SETTINGS');
  return {topic, difficulty:input.difficulty, questionCount:input.questionCount, timerSeconds:input.timerSeconds,
    additionalInstructions:text(input.additionalInstructions ?? '',0,500,'INVALID_INSTRUCTIONS')};
}
export function outputSchema(count) {
  return {type:'object',additionalProperties:false,required:['questions'],properties:{questions:{type:'array',minItems:count,maxItems:count,items:{
    type:'object',additionalProperties:false,required:['text','options','correctIndex','explanation'],properties:{
      text:{type:'string',minLength:5,maxLength:600}, options:{type:'array',minItems:4,maxItems:4,items:{type:'string',minLength:1,maxLength:240}},
      correctIndex:{type:'integer',minimum:0,maximum:3}, explanation:{type:'string',minLength:10,maxLength:1000}
    }}}}};
}
export function validateOutput(output, request) {
  try {
    if (!output || Object.keys(output).length !== 1 || !Array.isArray(output.questions) || output.questions.length !== request.questionCount) throw Error();
    const seen = new Set();
    return output.questions.map(q => {
      if (!q || Object.keys(q).sort().join() !== 'correctIndex,explanation,options,text') throw Error();
      const prompt = text(q.text,5,600,'MALFORMED_QUESTIONS'), key = normalize(prompt);
      if (seen.has(key)) throw Error(); seen.add(key);
      if (!Array.isArray(q.options) || q.options.length !== 4) throw Error();
      const options = q.options.map(o => text(o,1,240,'MALFORMED_QUESTIONS'));
      if (new Set(options.map(normalize)).size !== 4 || !Number.isInteger(q.correctIndex) || q.correctIndex < 0 || q.correctIndex > 3) throw Error();
      const explanation = text(q.explanation,10,1000,'MALFORMED_QUESTIONS');
      if (normalize(explanation) === key) throw Error();
      return {text:prompt,options,correctIndex:q.correctIndex,explanation,topic:request.topic,difficulty:request.difficulty};
    });
  } catch { throw new ApiError(502,'MALFORMED_QUESTIONS'); }
}

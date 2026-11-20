
'use strict';

class FrontendTransitionError extends Error {}

const imageId = /^sha256:[a-f0-9]{64}$/;
const frontendActions = ['update-frontend', 'rollback-frontend', 'recover-frontend'];

function validateFrontendImage(image, id, current, base, run) {
  const fields = ['Entrypoint','Cmd','User','WorkingDir','Env','ExposedPorts','Volumes','Healthcheck','StopSignal'];
  const layers = base.RootFS?.Layers;
  if (image.Id !== id || image.Os !== 'linux' || image.Architecture !== 'amd64'
      || image.Config?.Labels?.['gestaoacoes.operacional.run'] !== run
      || !layers?.length || !layers.every((layer,i) => image.RootFS?.Layers?.[i] === layer)
      || fields.some(key => JSON.stringify(image.Config[key]) !== JSON.stringify(current.Config[key])))
    throw new FrontendTransitionError('Imagem candidata incompativel com o frontend aprovado');
}

// The receipt is the journal. Persist intent before changing the canonical tag.
async function frontendTransition(action, option, state, io) {
  const pending = state.pendingFrontend;
  const current = state.imageIds.frontend;
  if (!imageId.test(current) || !frontendActions.includes(action)) throw new FrontendTransitionError('Recibo ou acao de frontend invalido');
  if (pending && action !== 'recover-frontend') throw new FrontendTransitionError('Transicao de frontend pendente; use recover-frontend');
  if (!pending && action === 'recover-frontend') throw new FrontendTransitionError('Nenhuma transicao de frontend para recuperar');

  let change = pending;
  if (change) {
    if (!['update-frontend','rollback-frontend'].includes(change.action)
        || change.from !== current || !imageId.test(change.to) || change.to === current || !change.services)
      throw new FrontendTransitionError('Recibo pendente de frontend invalido');
  } else {
    const to = action === 'update-frontend' ? option : state.previousFrontend;
    if (!imageId.test(to) || to === current) throw new FrontendTransitionError('Informe um ID sha256 distinto ou use um recibo de rollback valido');
    const live = await io.inspect();
    if (live.tag !== current || live.frontend?.image !== current || !live.frontend.running)
      throw new FrontendTransitionError('Frontend atual diverge do recibo');
    await io.validateImage(to, current);
    const retained = await io.retain(current);
    if (retained !== current) throw new FrontendTransitionError('Referencia de rollback divergente');
    change = {action, from:current, to, services:live.services};
    io.save({...state, pendingFrontend:change});
  }

  const target = action === 'recover-frontend' && change.action === 'update-frontend' ? change.from : change.to;
  const verifyState = live => {
    if (JSON.stringify(live.services) !== JSON.stringify(change.services))
      throw new FrontendTransitionError('Backend ou PostgreSQL divergente do inicio da transicao');
    if (![change.from,change.to].includes(live.tag)
        || (live.frontend && ![change.from,change.to].includes(live.frontend.image)))
      throw new FrontendTransitionError('Imagem ou container de frontend desconhecido');
  };
  const live = await io.inspect();
  verifyState(live);
  await io.validateImage(target, change.from);
  if (live.tag !== target) await io.tag(target);
  if (live.frontend?.image !== target || !live.frontend.running) await io.reconcile();

  const after = await io.inspect();
  verifyState(after);
  if (after.tag !== target || after.frontend?.image !== target || !after.frontend.running)
    throw new FrontendTransitionError('Frontend nao reconciliado com a imagem esperada');
  await io.ready();
  const final = await io.inspect();
  verifyState(final);
  if (final.tag !== target || final.frontend?.image !== target || !final.frontend.running)
    throw new FrontendTransitionError('Frontend mudou durante a verificacao');
  const receipt = {...state, imageIds:{...state.imageIds, frontend:target}};
  delete receipt.pendingFrontend;
  if (target !== change.from) receipt.previousFrontend = change.from;
  io.save(receipt);
  return target;
}
module.exports = {frontendActions,frontendTransition,validateFrontendImage,imageId,FrontendTransitionError};

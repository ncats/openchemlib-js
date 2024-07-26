import Benchmark from 'benchmark';

import OCLOld from '../distold/openchemlib-full.pretty.js';
import OCLNew from '../full.pretty.js';

const idcode = 'enYXNH@MHDAELem`OCIILdhhdiheCDlieKDdefndZRVVjjfjjfjihJBbb@@@';
const mol = OCLNew.Molecule.fromIDCode(idcode);
const molfile = mol.toMolfile();

const suite = new Benchmark.Suite();

suite
  .add('old', () => {
    OCLNew.Molecule.fromMolfile(molfile);
  })
  .add('new', () => {
    OCLOld.Molecule.fromMolfile(molfile);
  })
  .on('cycle', (event) => {
    console.log(String(event.target));
  })
  .on('complete', function onComplete() {
    console.log(`Fastest is ${suite.filter('fastest').map('name')}`);
  })
  .run();

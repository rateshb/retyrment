// Simple import check to identify issues

async function testImports() {
  try {
    await import('./pages/Dashboard');
  } catch (error) {
  }

  try {
    await import('./pages/Income');
  } catch (error) {
  }

  try {
    await import('./pages/InsuranceRecommendations');
  } catch (error) {
  }
}

testImports();

// Simple import check to identify issues
console.log('Testing imports...');

async function testImports() {
  try {
    console.log('Testing Dashboard import...');
    const Dashboard = await import('./pages/Dashboard');
    console.log('✅ Dashboard imported successfully');
  } catch (error) {
    console.error('❌ Dashboard import failed:', error.message);
  }

  try {
    console.log('Testing Income import...');
    const Income = await import('./pages/Income');
    console.log('✅ Income imported successfully');
  } catch (error) {
    console.error('❌ Income import failed:', error.message);
  }

  try {
    console.log('Testing InsuranceRecommendations import...');
    const InsuranceRecommendations = await import('./pages/InsuranceRecommendations');
    console.log('✅ InsuranceRecommendations imported successfully');
  } catch (error) {
    console.error('❌ InsuranceRecommendations import failed:', error.message);
  }
}

testImports();
